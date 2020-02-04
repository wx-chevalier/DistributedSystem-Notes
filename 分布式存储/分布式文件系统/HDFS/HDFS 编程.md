```java
public class TestHDFSFile {
  private String localPath = "C:/D/JavaWorkSpace/bigdata/temp/";
  private String hdfsPath = "hdfs://192.168.2.6:9000/user/hadoop/temp/";

  public static void main(String[] args) throws Exception {
    // new TestHDFSFile().testUpload();
    // new TestHDFSFile().testCreate();
    //new TestHDFSFile().testRename();
    //new TestHDFSFile().testDel();
    //new TestHDFSFile().testgetModifyTime();
    //new TestHDFSFile().testExists();
    //new TestHDFSFile().testFileBlockLocation();
    new TestHDFSFile().testGetHostName();
  }

  // 上传本地文件到HDFS
  public void testUpload() throws Exception {
    Configuration conf = new Configuration();

    // conf.addResource(new Path(localPath + "core-site.xml"));
    FileSystem hdfs = FileSystem.get(conf);
    Path src = new Path(localPath + "file01.txt");
    Path dst = new Path(hdfsPath);
    hdfs.copyFromLocalFile(src, dst);

    System.out.println("Upload to " + conf.get("fs.default.name"));
    FileStatus files[] = hdfs.listStatus(dst);
    for (FileStatus file : files) {
      System.out.println(file.getPath());
    }
  }

  // 创建HDFS文件
  public void testCreate() throws Exception {
    Configuration conf = new Configuration();
    byte[] buff = "hello world!".getBytes();

    FileSystem hdfs = FileSystem.get(conf);
    Path dst = new Path(hdfsPath + "hello.txt");
    FSDataOutputStream outputStream = null;
    try {
      outputStream = hdfs.create(dst);
      outputStream.write(buff, 0, buff.length);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (outputStream != null) {
        outputStream.close();
      }
    }

    FileStatus files[] = hdfs.listStatus(dst);
    for (FileStatus file : files) {
      System.out.println(file.getPath());
    }
  }

  // 重命名HDFS文件
  public void testRename() throws Exception {
    Configuration conf = new Configuration();

    FileSystem hdfs = FileSystem.get(conf);
    Path dst = new Path(hdfsPath);

    Path frpath = new Path(hdfsPath + "hello.txt");
    Path topath = new Path(hdfsPath + "hello2.txt");

    hdfs.rename(frpath, topath);

    FileStatus files[] = hdfs.listStatus(dst);
    for (FileStatus file : files) {
      System.out.println(file.getPath());
    }
  }

  // 刪除HDFS文件
  public void testDel() throws Exception {
    Configuration conf = new Configuration();

    FileSystem hdfs = FileSystem.get(conf);
    Path dst = new Path(hdfsPath);

    Path topath = new Path(hdfsPath + "hello2.txt");

    boolean ok = hdfs.delete(topath, false);
    System.out.println(ok ? "删除成功" : "删除失败");

    FileStatus files[] = hdfs.listStatus(dst);
    for (FileStatus file : files) {
      System.out.println(file.getPath());
    }
  }

  // 查看HDFS文件的最后修改时间
  public void testgetModifyTime() throws Exception {
    Configuration conf = new Configuration();

    FileSystem hdfs = FileSystem.get(conf);
    Path dst = new Path(hdfsPath);

    FileStatus files[] = hdfs.listStatus(dst);
    for (FileStatus file : files) {
      System.out.println(file.getPath() + "\t" + file.getModificationTime());

      System.out.println(
        file.getPath() + "\t" + new Date(file.getModificationTime())
      );
    }
  }

  // 查看HDFS文件是否存在
  public void testExists() throws Exception {
    Configuration conf = new Configuration();

    FileSystem hdfs = FileSystem.get(conf);
    Path dst = new Path(hdfsPath + "file01.txt");

    boolean ok = hdfs.exists(dst);
    System.out.println(ok ? "文件存在" : "文件不存在");
  }

  // 查看某个文件在HDFS集群的位置
  public void testFileBlockLocation() throws Exception {
    Configuration conf = new Configuration();

    FileSystem hdfs = FileSystem.get(conf);
    Path dst = new Path(hdfsPath + "file01.txt");

    FileStatus fileStatus = hdfs.getFileStatus(dst);
    BlockLocation[] blockLocations = hdfs.getFileBlockLocations(
      fileStatus,
      0,
      fileStatus.getLen()
    );
    for (BlockLocation block : blockLocations) {
      System.out.println(
        Arrays.toString(block.getHosts()) +
          "\t" +
          Arrays.toString(block.getNames())
      );
    }
  }

  // 获取HDFS集群上所有节点名称
  public void testGetHostName() throws Exception {
    Configuration conf = new Configuration();

    DistributedFileSystem hdfs = (DistributedFileSystem) FileSystem.get(conf);
    DatanodeInfo[] dataNodeStats = hdfs.getDataNodeStats();

    for (DatanodeInfo dataNode : dataNodeStats) {
      System.out.println(dataNode.getHostName() + "\t" + dataNode.getName());
    }
  }
}
```
