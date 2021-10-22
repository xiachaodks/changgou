package com.changgou.util;

import com.changgou.file.FastDFSFile;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

public class FastDFSUtil {

    /**
     * 加载Tracker连接信息
     */
    static {
        try {
            String filename = new ClassPathResource("fdfs_client.conf").getPath();
            ClientGlobal.init(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 文件上传
     *
     * @param fastDFSFile
     * @return
     */
    public static String[] upload(FastDFSFile fastDFSFile) throws Exception {
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();
        //创建StorageClient对象，存储StorageClient链接信息
        StorageClient storageClient = getStorageClient();
        /**
         * uploads[]:
         *      uploads[0]:文件上传所存储的组名称 group1
         *      uploads[1]:文件存储的名称  M00/0/0/i.jpg
         */
        String[] uploads = storageClient.upload_file(fastDFSFile.getContent(), fastDFSFile.getExt(), null);
        return uploads;
    }

    /**
     * 获取文件详细信息
     *
     * @param groupName
     * @param remoteName
     * @return
     * @throws Exception
     */
    public static FileInfo getFileInfo(String groupName, String remoteName) throws Exception {
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();
        //创建StorageClient对象，存储StorageClient链接信息
        StorageClient storageClient = getStorageClient();
        return storageClient.get_file_info(groupName, remoteName);
    }

    /**
     * 文件下载
     *
     * @param groupName
     * @param remoteName
     * @return
     * @throws Exception
     */
    public static byte[] downLoadFile(String groupName, String remoteName) throws Exception {
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();
        //创建StorageClient对象，存储StorageClient链接信息
        StorageClient storageClient = new StorageClient(trackerServer, null);
        return storageClient.download_file(groupName, remoteName);
    }

    /**
     * 文件删除
     *
     * @param groupName
     * @param remoteName
     * @throws Exception
     */
    public static void deleteFile(String groupName, String remoteName) throws Exception {
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();
        //创建StorageClient对象，存储StorageClient链接信息
        StorageClient storageClient = getStorageClient();
        storageClient.delete_file(groupName, remoteName);
    }

    /**
     * 获取Storage信息
     *
     * @return
     */
    public static StorageServer getStorage() throws Exception {
        //创建一个Tracker客户端连接对象TrackerClient
        TrackerClient trackerClient = new TrackerClient();
        //通过TrackerClient对象访问TrackerServer，获取Storage信息
        TrackerServer trackerServer = trackerClient.getConnection();
        return trackerClient.getStoreStorage(trackerServer);
    }

    /**
     * 获取Storage的IP和端口信息
     *
     * @return
     */
    public static ServerInfo[] getServerInfo(String groupName, String remoteName) throws Exception {
        //创建一个Tracker客户端连接对象TrackerClient
        TrackerClient trackerClient = new TrackerClient();
        //通过TrackerClient对象访问TrackerServer，获取Storage信息
        TrackerServer trackerServer = trackerClient.getConnection();
        return trackerClient.getFetchStorages(trackerServer, groupName, remoteName);
    }

    /**
     * 获取Tracker的信息
     *
     * @return
     */
    public static String getTrackerInfo() throws Exception {
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();
        //Tracker的IP，HTTP端口
        String ip = trackerServer.getInetSocketAddress().getHostString();//ip
        int g_tracker_http_port = ClientGlobal.getG_tracker_http_port();//port
        return "http://" + ip + ":" + g_tracker_http_port;
    }

    /**
     * 返回trackerServer
     *
     * @return
     * @throws Exception
     */
    public static TrackerServer getTrackerServer() throws Exception {
        //创建一个Tracker客户端连接对象TrackerClient
        TrackerClient trackerClient = new TrackerClient();
        //通过TrackerClient对象访问TrackerServer，获取Storage信息
        TrackerServer trackerServer = trackerClient.getConnection();
        return trackerServer;
    }

    /***
     * 获取StorageClient
     * @return
     * @throws Exception
     */
    public static StorageClient getStorageClient() throws Exception {
        //获取TrackerServer
        TrackerServer trackerServer = getTrackerServer();
        //通过TrackerServer创建StorageClient
        StorageClient storageClient = new StorageClient(trackerServer, null);
        return storageClient;
    }
}
