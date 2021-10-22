package com.changgou.controller;

import entity.Result;
import entity.StatusCode;
import com.changgou.file.FastDFSFile;
import com.changgou.util.FastDFSUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin
@RequestMapping("/upload")
public class FileUpLoadController {

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping
    public Result upload(@RequestParam(value = "file") MultipartFile file) throws Exception {
        FastDFSFile fastDFSFile = new FastDFSFile(file.getOriginalFilename(), file.getBytes(),
                StringUtils.getFilenameExtension(file.getOriginalFilename()));
        String[] uploads = FastDFSUtil.upload(fastDFSFile);
        //拼接url
       //String url = "http://192.168.163.133:8080/"+uploads[0]+"/"+uploads[1];
        String url = FastDFSUtil.getTrackerInfo()+uploads[0]+"/"+uploads[1];
        return new Result(true, StatusCode.OK, "上传成功",url);
    }
}
