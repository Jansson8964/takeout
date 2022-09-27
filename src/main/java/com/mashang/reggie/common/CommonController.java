package com.mashang.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;


/**
 * 文件上传和下载
 */
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {
    @Value("${reggie.path}") // 与参数名保持一致就能读取到了
    private String basePath;

    /**
     * 文件上传
     * 参数名要和form data里面的值保持一致
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    // 上传文件需要在这里声明multipartfile这个参数
    public R<String> upload(MultipartFile file) {
        // file是一个临时文件,需要转存到指定位置,否则本次请求完成后临时文件会删除
        log.info(file.toString());
        // 原始文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + suffix;
        // 创建一个文件目录
        File dir = new File(basePath);
        // 判断当前目录是否存在
        if (!dir.exists()) {
            // 目录不存在,需要创建
            dir.mkdirs();
        }
        try {
            // 为了防止覆盖文件.要用UUID生成一个新的文件名
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //return null;
        // 给页面返回文件名
        return R.success(fileName);
    }

    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) {
        try {

            /**
             * 输入流先上传到服务器,然后服务器再输出流将页面写回到浏览器
             */
            // 输入流,通过输入流读取文件内容
            // 找到当前文件,通过输入流读取内容,上传到服务器,浏览器通过服务器加载
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));
            // 输出流,通过输出流将文件写回浏览器,在浏览器展示图片
            // 此时电脑相当于服务器,我们要向浏览器写回数据,就要用response对象
            ServletOutputStream outputStream = response.getOutputStream();
            // 设置响应回去的是什么类型的文件
            response.setContentType("image/jpeg");
            int len = 0;
            byte[] bytes = new byte[1024];
            /***
             * byte[1024]就是以byte为单位,在内存中连续开辟1024个
             * new String(byte, 0, len); //这里的0是什么意思
             * 这是将字节数组中角标为 0 到角标为 len-1 转化为字符串。
             * 第一个bytes参数就是你定义的数据名；
             * 第二个0就是从数组里角标为0(也就是第一位)开始转换字符串；
             * 第三个len就是你读取文件所读到的字节个数。
             */
            while ((len = fileInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
                outputStream.flush();
            }
            // 关闭资源
            outputStream.close();
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
