package plus.maa.backend.controller;

import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaResultException;

import java.io.IOException;
import java.io.InputStream;

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

    @PostMapping("/upload")
    public MaaResult<String> uploadFile(MultipartFile file) {
        if (file.getSize() < 1000) {
            throw new MaaResultException("非法的文件大小");
        }
        Assert.notNull(file.getOriginalFilename(), "文件名不可为空");

        try {
            gridFsOperations.store(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return MaaResult.success();
    }

    @GetMapping("/download")
    public MaaResult<String> fileDownload(String date, HttpServletResponse response) throws IOException {
        GridFSFindIterable files = gridFsOperations.find(new Query(Criteria.where("uploadDate").lt(date)));
        //返回压缩包
        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        response.addHeader("Content-Disposition", "attachment;filename=" + System.currentTimeMillis() + "zip");
        response.setContentType("application/octet-stream");

        for (GridFSFile file : files) {
            GridFsResource gridFsResource = gridFsOperations.getResource(file);
            ZipEntry zipEntry = new ZipEntry(file.getFilename());

            zipOutputStream.putNextEntry(zipEntry);
            InputStream is = gridFsResource.getInputStream();

            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                zipOutputStream.write(bytes, 0, len);
                zipOutputStream.flush();
            }
            is.close();
        }
        zipOutputStream.close();
        return null;
    }
}
