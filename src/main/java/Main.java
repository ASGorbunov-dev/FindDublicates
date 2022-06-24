import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {
        long time = System.currentTimeMillis();
        String startDirectory = null;
        if (args.length > 0) {
            startDirectory = args[0];
            if (Files.exists(Paths.get(startDirectory)) && Files.isDirectory(Paths.get(startDirectory))) {
                List<Path> fileList = Files.walk(Paths.get(startDirectory)).filter(Files::isRegularFile).collect(Collectors.toList());
                ConcurrentHashMap<String, List<Path>> hashFiles = new ConcurrentHashMap<>();
                fillMap(fileList, hashFiles);
                Long fullSize = getFullSize(fileList);
                Long sizeWithoutDublicate = getSizeWithoutDublicate(hashFiles);
                System.out.println("Количество байт освободится после удаления: " + (fullSize - sizeWithoutDublicate));
                System.out.println("Затраченное время на работу в мс: " + (System.currentTimeMillis() - time));
            } else {
                System.err.println("Не верно задана стартовая директория!");
                System.exit(1);
            }
        } else {
            System.err.println("Не задано необходимых параметров!");
            System.exit(1);
        }
    }

    public static Long getFullSize(List<Path> fileList) {
        return fileList.stream().mapToLong(path -> {
            try {
                return Files.size(path);
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }).sum();
    }

    public static Long getSizeWithoutDublicate(Map<String, List<Path>> hashFiles) {
        return hashFiles.keySet().stream().mapToLong(hash -> {
            try {
                return Files.size(hashFiles.get(hash).get(0));
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }).sum();
    }

    public static void fillMap(List<Path> fileList, Map<String, List<Path>> hashFiles) throws InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(16);
        for (Path path : fileList) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String hash = HashFile.getFileChecksum(path.toString());
                        if (hashFiles.containsKey(hash)) {
                            hashFiles.get(hash).add(path);
                        } else {
                            List<Path> list = new ArrayList<>();
                            list.add(path);
                            hashFiles.put(hash, list);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(5, TimeUnit.SECONDS);

//        fileList.stream().parallel().forEach(path -> {
//            try{
//                String hash = HashFile.getFileChecksum(path.toString());
//                if (hashFiles.containsKey(hash)){
//                    hashFiles.get(hash).add(path);
//                } else {
//                    List<Path> list = new ArrayList<>();
//                    list.add(path);
//                    hashFiles.put(hash,list);
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        });
    }
}
