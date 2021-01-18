package org.conch;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Test {
    public static void Uncompress(String inputFile) throws Exception {
        File srcFile = new File(inputFile);//获取当前压缩文件
        // 判断源文件是否存在
        if (!srcFile.exists()) {
            throw new Exception(srcFile.getPath() + "所指文件不存在");
        }
        //开始解压
        SevenZFile zIn = new SevenZFile(srcFile);
        SevenZArchiveEntry entry = null;
        File file = null;
        while ((entry = zIn.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                System.out.println(entry.getName());
//                String str = entry.getName();
//                if(str.contains(VERSION_SPILLER) &&  str.contains("jar")){
//                    int lastFileSepIndex = str.lastIndexOf(File.separator);
//                    int startIndex = (lastFileSepIndex == -1) ? 0 : lastFileSepIndex + 1;
//                    System.out.println("removeVersion:===>"+str.substring(startIndex,str.lastIndexOf(VERSION_SPILLER)));
//                }
//                if(str.contains(PATH_SPILLER)){
//                    System.out.println("removePath:===>"+str.substring(str.lastIndexOf(PATH_SPILLER) + 1, str.length()));
//                }
                file = new File(entry.getName());
                if (!file.exists()) {
                    new File(file.getParent()).mkdirs();//创建此文件的上级目录
                }
                OutputStream out = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(out);
                int len = -1;
                byte[] buf = new byte[1024];
                while ((len = zIn.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                // 关流顺序，先打开的后关闭
                bos.close();
                out.close();
            }
        }
    }
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    static final String BAK_FOLDER = Paths.get(".","bak").toString();

    private static final String VERSION_SPILLER = "-";
    private static final String PATH_SPILLER = "/";

    private static List<String> strs;

    private static void  add(){
        strs.add("123");
        strs.add("987");
    }

    public static void main(String[] args) {
//        try {
//            Uncompress("temp/cos-0.1.0.7z");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        String timeStr = dateFormat.format(System.currentTimeMillis());
//        String bakFolder = Paths.get(BAK_FOLDER).resolve("Test" + "_" + timeStr).toString();
//        System.out.println(bakFolder);

//        long fileSeparatorCount = "".chars().filter(c -> c == '/').count();
//        System.out.println(fileSeparatorCount);


//        String str = "conch/lib/commons-compress-0.4.1.jar";
//        if(str.contains(PATH_SPILLER)){
//            System.out.println( str.substring(str.lastIndexOf(PATH_SPILLER) + 1, str.length()));
//            str = str.substring(str.lastIndexOf(PATH_SPILLER) + 1, str.length());
//        }
//        if(str.contains(VERSION_SPILLER)){
//            int lastFileSepIndex = str.lastIndexOf(File.separator);
//            int startIndex = (lastFileSepIndex == -1) ? 0 : lastFileSepIndex + 1;
//            System.out.println(str.substring(startIndex,str.lastIndexOf(VERSION_SPILLER)));
//        }


//        try {
//            File file = new File("commons-compress-1.20");
//            File zipFile = new File("commonsZip.zip");
//            InputStream input = new FileInputStream(file);
//            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
//            zipOut.putNextEntry(new ZipEntry(file.getName()));
//            int temp = 0;
//            while((temp = input.read()) != -1){
//                zipOut.write(temp);
//            }
//            input.close();
//            zipOut.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        Path appRootPath = Paths.get(".");
//        System.out.println(appRootPath.toString());

//        File file = new File("LICENSE");
//        File parent = file.getParentFile();
//        System.out.println(file.getParentFile());
//        if(!parent.exists()){
//            System.out.println(parent.getName() + " mkdirs");
//            parent.mkdirs();
//        }


//        Path path = Paths.get(".");
//
//        // create an object of Path
//        // to pass to resolve method
//        Path path2 = Paths.get("..\\workspace");
//
//        // call resolve()
//        // to create resolved Path
//        Path resolvedPath = path.resolve("mw_test_db");
//
//        // print result
//        System.out.println("Resolved Path:" + resolvedPath);

//        strs = new ArrayList<>();
//        strs.add("456");
//        System.out.println(strs.hashCode());
//        add();
//        System.out.println(strs);
        String a = "qq";
        try{
            if(true){
                int i = 1/0;
                System.out.println(i);
            }
            a += "email";
        }catch (Exception e){
            try {
                if(true){
                    int[] ints = {1, 2};
                    System.out.println(ints[3]);
                }
                a += "music";
            }catch (Exception e1){
                System.out.println(e1);
                return;
            }
        }


        System.out.println(a);
    }
}
