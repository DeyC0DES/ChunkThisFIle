
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ChunkThisFile {

    public static void zipFolder(File folder) throws IOException {
        File zipFile = new File(folder.getParent() + "\\" + "fileZipped.zip");

        if (folder.isFile()) {
            return;
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            System.out.print(" . Exceuting zipFolderHelper!");
            zipFolderHelper(folder, folder.getName(), zos);
        }
        System.out.println(" . File successfully zip!");
    }

    public static void zipFolderHelper(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            System.out.println(" . File is empty!");
            zos.putNextEntry(new ZipEntry(parentFolder + "/"));
            zos.closeEntry();
        } else {
            System.out.println(" . Files founds!");
            for (File file : files) {
                System.out.println(" . Processing . . .");
                String zipEntryname = parentFolder + "/" + file.getName();
                if (file.isDirectory()) {
                    zipFolderHelper(file, zipEntryname, zos);
                } else {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        zos.putNextEntry(new ZipEntry(zipEntryname));
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    }
                }
            }
        }

        System.out.println(" . OK! FINISHED!");
    }

    public static void splitFile(File file, int chunkSize) throws IOException {

        File chunksFolder = new File(file + "\\files");
        File chunks = new File(file + "\\files\\chunks");

        if (!chunksFolder.exists()) {
            chunksFolder.mkdirs();
            chunks.mkdirs();
        }
        
        File zippedFile = new File(file.getAbsolutePath() + "\\fileZipped.zip");

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(zippedFile))) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            int partCounter = 1;

            System.out.println(" . Start split!");

            while ((bytesRead = bis.read(buffer)) > 0) {
                File newFile = new File(chunks, "chunk.part" + partCounter++);
                System.out.println(" . Processing . . .");
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile))) {
                    bos.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public static void rebuild(File[] files, File targetFile)throws IOException {

        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                String name1 = f1.getName().replace("chunk.part", "");
                String name2 = f2.getName().replace("chunk.part", "");
                int num1 = Integer.parseInt(name1);
                int num2 = Integer.parseInt(name2);
                return Integer.compare(num1, num2);
            }
        });

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile))) {
            for (File file : files) {
                System.out.print("Verify: " + file.getName());
                System.out.print(".");
                System.out.print(" .");
                System.out.print(" . ");
                if (file.isFile()) {
                    System.out.print(" Its a file! ");
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) > 0) {
                            bos.write(buffer, 0, bytesRead);
                        }
                        System.out.println("Byte read!");
                    }
                }
            }
        }

    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        space();
        System.out.println("+--------------------------------------- CHUNK THIS FILE ---------------------------------------+");
        System.out.println();
        System.out.print("  . Chunk a File or Rebuild [C/R]? ");
        String process = scanner.next().toUpperCase();

        if (!process.equals("C") && !process.equals("R")) {
            System.out.println(" . invalid option!");
            return;
        }

        System.out.println();
        
        System.out.println(" . Zip the folder to make the process faster");
        System.out.println(" . On rebuild select the chunks folder!");
        System.out.println(" . Its recommended place the folders in a single!");
        System.out.println(" . If you want to chunk a file, just put the path.");
        System.out.println();
        System.out.print("  . Folder: ");
        String folderPATH = scanner.next();
        int chunkSize = 0;
        if (process.equals("C")) {
            System.out.println();
            System.out.println(" 1. 1MB");
            System.out.println(" 2. 4MB");
            System.out.println(" 3. 25MB");
            System.out.println(" 4. 100MB");
            System.out.println(" 5. 2.5GB");
            System.out.println();
            System.out.print(" . Select a chunk size: ");
            chunkSize = 1024  * 1024;
            switch (scanner.next()) {
                case "1" -> chunkSize = 1024 * 1024;
                case "2" -> chunkSize = 2024 * 2024;
                case "3" -> chunkSize = 5024 * 5024;
                case "4" -> chunkSize = 10024 * 10024;
                case "5" -> chunkSize = 50024 * 50024;
                default -> System.out.println(" . Invalid option, defail value 1MB setted!");   
            }
        }
        System.out.println("+-----------------------------------------------------------------------------------------------+");
        space();

        File folder = new File(folderPATH);
        try {
            if (process.equals("C")) {
                zipFolder(folder);
                splitFile(folder.getParentFile(), chunkSize);
                System.out.println(" . zip successfully split!");
            } else if (process.equals("R")) {
                File rebuildFolder = new File(folder.getAbsolutePath() + "\\rebuild.zip");

                if (!folder.exists()) {
                    System.out.println(" . Files/chunks folder dont exist, verify the folder and try again.");
                    scanner.close();
                    return;
                }

                File[] files = folder.listFiles();
                
                for (File file : files) {
                    if (!file.getName().contains(".part")) {
                        List<File> tempList = new ArrayList<>(Arrays.asList(files));
                        tempList.remove(file);
                        files = tempList.toArray(new File[0]);
                    }
                }

                if (files == null || files.length == 0) {
                    System.out.println(" . Folder is empty or the parts cant be found.");
                }

                rebuild(files, rebuildFolder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        scanner.close();
    }

    public static void space() {
        for (int i = 0; i < 25; i++) {
            System.out.println();
        }
    }
}
