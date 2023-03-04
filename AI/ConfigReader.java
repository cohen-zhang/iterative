import java.sql.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConfigReader {
    private ThreadPoolExecutor executor;
    private Connection conn;
    private String config;

    public ConfigReader() {
        // 初始化连接
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydatabase", "root", "password");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT config FROM configs ORDER BY id DESC LIMIT 5");
            while (rs.next()) {
                String newConfig = rs.getString("config");
                if (!newConfig.equals(config)) {
                    config = newConfig;
                    // 配置发生变更，进行相应的处理
                    System.out.println("Config changed: " + config);
                }
            }
            rs.close();
            stmt.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        // 每3秒读取最新配置
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ConfigReaderThreadFactory());
        executor.execute(() -> {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT config FROM configs ORDER BY id DESC LIMIT 5");
                while (rs.next()) {
                    String newConfig = rs.getString("config");
                    if (!newConfig.equals(config)) {
                        config = newConfig;
                        // 配置发生变更，进行相应的处理
                        System.out.println("Config changed: " + config);
                    }
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT config FROM configs ORDER BY id DESC LIMIT 5");
                while (rs.next()) {
                    String newConfig = rs.getString("config");
                    if (!newConfig.equals(config)) {
                        config = newConfig;
                        // 配置发生变更，进行相应的处理
                        System.out.println("Config changed: " + config);
                    }
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    public String getConfig() {
        return config;
    }

    public void stop() {
        // 停止定时器和连接
        executor.shutdown();
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class ConfigReaderThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("config-reader-thread");
            return t;
        }
    }
}
