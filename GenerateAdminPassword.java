import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateAdminPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String hashedPassword = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt hash: " + hashedPassword);
        System.out.println("Add this to init.sql:");
        System.out.println("INSERT INTO users (username, password, email, role) VALUES");
        System.out.println("('admin', '" + hashedPassword + "', 'admin@example.com', 'ADMIN');");
    }
}