package plus.maa.backend.service;


import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import plus.maa.backend.controller.file.ImageDownloadDTO;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.RedisCache;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author LoMu
 * Date  2023-04-16 23:21
 */

@RequiredArgsConstructor
@Service
public class FileService {
    private final GridFsOperations gridFsOperations;
    private final RedisCache redisCache;

    public void uploadFile(MultipartFile file,
                           String type,
                           String version,
                           String classification,
                           String label,
                           String ip) {

        //redis持久化
        if (redisCache.getCache("NotEnable:UploadFile", String.class) != null) {
            throw new MaaResultException(403, "closed uploadfile");
        }

        //文件小于1024Bytes不接收
        if (file.getSize() < 1024) {
            throw new MultipartException("Minimum upload size exceeded");
        }
        Assert.notNull(file.getOriginalFilename(), "文件名不可为空");

        String antecedentVersion = null;
        if (version.contains("-")) {
            String[] split = version.split("-");
            version = split[0];
            antecedentVersion = split[1];
        }

        Document document = new Document();
        document.put("version", version);
        document.put("antecedentVersion", antecedentVersion);
        document.put("label", label);
        document.put("classification", classification);
        document.put("type", type);
        document.put("ip", ip);

        int index = file.getOriginalFilename().lastIndexOf(".");
        String fileType = "";
        if (index != -1) {
            fileType = file.getOriginalFilename().substring(index);
        }

        String fileName = "Maa-" + UUID.randomUUID().toString().replaceAll("-", "") + fileType;

        try {
            gridFsOperations.store(file.getInputStream(), fileName, document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void downloadDateFile(String date, String beLocated, boolean delete, HttpServletResponse response) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date d;
        Query query;

        if (StringUtils.isBlank(date)) {
            d = new Date(System.currentTimeMillis());
        } else {
            try {
                d = formatter.parse(date);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        if (StringUtils.isBlank(beLocated) || Objects.equals("after", beLocated.toLowerCase())) {
            query = new Query(Criteria.where("metadata").gte(d));
        } else {
            query = new Query(Criteria.where("uploadDate").lte(d));
        }
        GridFSFindIterable files = gridFsOperations.find(query);

        response.addHeader("Content-Disposition", "attachment;filename=" + System.currentTimeMillis() + ".zip");

        gzip(response, files);

        if (delete) {
            gridFsOperations.delete(query);
        }
    }


    public void downloadFile(ImageDownloadDTO imageDownloadDTO, HttpServletResponse response) {
        Query query = new Query();
        Set<Criteria> criteriaSet = new HashSet<>();


        //图片类型
        criteriaSet.add(GridFsCriteria.whereMetaData("type").regex(Pattern.compile(imageDownloadDTO.getType(), Pattern.CASE_INSENSITIVE)));

        //指定下载某个类型的图片
        if (StringUtils.isNotBlank(imageDownloadDTO.getClassification())) {
            criteriaSet.add(GridFsCriteria.whereMetaData("classification").regex(Pattern.compile(imageDownloadDTO.getClassification(), Pattern.CASE_INSENSITIVE)));
        }

        //指定版本或指定范围版本
        if (!Objects.isNull(imageDownloadDTO.getVersion())) {
            List<String> version = imageDownloadDTO.getVersion();

            if (version.size() == 1) {
                String antecedentVersion = null;
                if (version.get(0).contains("-")) {
                    String[] split = version.get(0).split("-");
                    antecedentVersion = split[1];
                }
                criteriaSet.add(GridFsCriteria.whereMetaData("version").is(version.get(0)).and("antecedentVersion").is(antecedentVersion));

            } else if (version.size() == 2) {
                criteriaSet.add(GridFsCriteria.whereMetaData("version").gte(version.get(0)).lte(version.get(1)));
            }
        }

        if (StringUtils.isNotBlank(imageDownloadDTO.getLabel())) {
            criteriaSet.add(GridFsCriteria.whereMetaData("label").regex(Pattern.compile(imageDownloadDTO.getLabel(), Pattern.CASE_INSENSITIVE)));
        }

        Criteria criteria = new Criteria().andOperator(criteriaSet);
        query.addCriteria(criteria);


        GridFSFindIterable gridFSFiles = gridFsOperations.find(query);

        response.addHeader("Content-Disposition", "attachment;filename=" + "Maa-" + imageDownloadDTO.getType() + ".zip");

        gzip(response, gridFSFiles);

        if (imageDownloadDTO.isDelete()) {
            gridFsOperations.delete(query);
        }

    }

    public String disable() {
        setUploadEnabled(false);
        return "已关闭";
    }

    public String enable() {
        setUploadEnabled(true);
        return "已启用";
    }

    public boolean isUploadEnabled() {
        return redisCache.getCache("NotEnable:UploadFile", String.class) == null;
    }

    /**
     * 设置上传功能状态
     * @param enabled 是否开启
     */
    public void setUploadEnabled(boolean enabled) {
        // Fixme: redis recovery solution should be added, or change to another storage
        if (enabled) {
            redisCache.removeCache("NotEnable:UploadFile");
        } else {
            redisCache.setCache("NotEnable:UploadFile", "1", 0, TimeUnit.DAYS);
        }
    }


    private void gzip(HttpServletResponse response, GridFSFindIterable files) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {

            for (GridFSFile file : files) {

                ZipEntry zipEntry = new ZipEntry(file.getFilename());
                try (InputStream inputStream = gridFsOperations.getResource(file).getInputStream()) {
                    //添加压缩文件
                    zipOutputStream.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = inputStream.read(bytes)) != -1) {
                        zipOutputStream.write(bytes, 0, len);
                        zipOutputStream.flush();
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
