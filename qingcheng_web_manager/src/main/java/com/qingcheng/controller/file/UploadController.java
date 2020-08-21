package com.qingcheng.controller.file;

import com.aliyun.oss.OSSClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private HttpServletRequest request;

    @PostMapping("/native")
    public String nativeUpload(@RequestParam("file") MultipartFile file){

        String path = request.getSession().getServletContext().getRealPath("img");
        String filePath = path + "/" + file.getOriginalFilename();
        File desFile = new File(filePath);
        if (!desFile.getParentFile().exists()){
            desFile.mkdirs();
        }

        try {
            file.transferTo(desFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "http://localhost:9101/img/" + file.getOriginalFilename();

    }

    @Autowired
    private OSSClient ossClient;

    @PostMapping("/oss")
    public String ossUpload(@RequestParam("file") MultipartFile file,String folder){
        //oss的仓库名称
        String bucketName = "yjlhz-qingchengplus";
        //保证上传文件名的唯一性
        String fileName = folder+"/"+UUID.randomUUID()+file.getOriginalFilename();
        try {
            //上传图片
            ossClient.putObject(bucketName,fileName,file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //返回图片的URL
        return "https://"+bucketName+".oss-cn-beijing.aliyuncs.com/"+fileName;
    }

}
