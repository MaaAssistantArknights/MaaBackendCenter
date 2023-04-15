package plus.maa.backend.controller;

import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import plus.maa.backend.common.annotation.AccessLimit;
import plus.maa.backend.config.SpringDocConfig;
import plus.maa.backend.controller.response.MaaResult;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author LoMu
 * Date  2023-03-31 16:41
 */

@RestController
@RequestMapping("file")
@RequiredArgsConstructor
public class FileController {

    private final GridFsOperations gridFsOperations;

    /**
     * 支持匿名
     *
     * @param file file
     * @return 上传成功, 数据已被接收
     */
    @AccessLimit
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MaaResult<String> uploadFile(@RequestParam(name = "file") MultipartFile file) {
        //文件小于1024Bytes不接收
        if (file.getSize() < 1024) {
            throw new MultipartException("Minimum upload size exceeded");
        }
        Assert.notNull(file.getOriginalFilename(), "文件名不可为空");

        String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String fileName = "Maa-" + UUID.randomUUID().toString().replaceAll("-", "") + fileType;

        try {
            gridFsOperations.store(file.getInputStream(), fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return MaaResult.success("上传成功,数据已被接收");
    }

    @Operation(summary = "下载文件")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/zip", schema = @Schema(type = "string", format = "binary"))
    )
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @AccessLimit
    @GetMapping("/download")
    public void fileDownload(
            @Parameter(description = "日期 yyy-MM-dd") String date,
            @Parameter(description = "在日期之前或之后[before,after]") String beLocated,
            @Parameter(description = "对查询到的数据进行删除") boolean delete,
            HttpServletResponse response
    ) {
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
            query = new Query(Criteria.where("uploadDate").gte(d));
        } else {
            query = new Query(Criteria.where("uploadDate").lte(d));
        }
        GridFSFindIterable files = gridFsOperations.find(query);

        response.addHeader("Content-Disposition", "attachment;filename=" + System.currentTimeMillis() + ".zip");

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
            if (delete) {
                gridFsOperations.delete(query);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
