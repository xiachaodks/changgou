package com.changgou.util.test;

import com.changgou.util.FastDFSUtil;
import org.csource.fastdfs.FileInfo;
import org.csource.fastdfs.ServerInfo;
import org.csource.fastdfs.StorageServer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileDFSTest {

    /**
     * 测试获取文件详细信息
     *
     * @throws Exception
     */
    @Test
    public void testGetFileInfo() throws Exception {
        FileInfo fileInfo = FastDFSUtil.getFileInfo("group1",
                "M00/00/00/wKijhWD6c2SAcTVeAAEKwn1SzsE543.png");
        System.out.printf(fileInfo.toString());
    }

    /**
     * 测试文件下载
     *
     * @throws Exception
     */
    @Test
    public void testDownLoadFile() throws Exception {
        byte[] bytes = FastDFSUtil.downLoadFile("group1",
                "M00/00/00/wKijhWD6c2SAcTVeAAEKwn1SzsE543.png");
        InputStream inputStream = new ByteArrayInputStream(bytes);
        byte[] buffer = new byte[1024];
        FileOutputStream fileOutputStream = new FileOutputStream("D:/1.jpg");
        while (inputStream.read(buffer) != -1) {
            fileOutputStream.write(buffer);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
        inputStream.close();
    }

    /**
     * 测试文件删除
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFile() throws Exception {
        FastDFSUtil.deleteFile("group1", "M00/00/00/wKijhWD6hYqAZjhrAB6s9KDfTnU601.jpg");
    }

    /**
     * 测试获取Storage信息
     *
     * @throws Exception
     */
    @Test
    public void testGetStorage() throws Exception {
        StorageServer storage = FastDFSUtil.getStorage();
        System.out.println(storage.getInetSocketAddress());
        System.out.println(storage.getStorePathIndex());
    }

    /**
     * 测试获取Storage的IP和端口
     *
     * @throws Exception
     */
    @Test
    public void testGetServerInfo() throws Exception {
        ServerInfo[] groups = FastDFSUtil.getServerInfo("group1", "M00/00/00/wKijhWD6cRyABQGaAAEKwn1SzsE591.png");
        for (ServerInfo group : groups) {
            System.out.println(group.getIpAddr());
            System.out.println(group.getPort());
        }
    }

    /**
     * 测试获取Tracker的信息
     *
     * @return
     */
    @Test
    public void testGetTrackerInfo() throws Exception {
        System.out.println(FastDFSUtil.getTrackerInfo());
    }
}
