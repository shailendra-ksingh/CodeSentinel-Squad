import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Deliberately flawed sample class used to demonstrate CodeSentinel.
 * Contains a mix of intentional issues: null-safety, resource leak,
 * broad exception handling, hardcoded secret, SQL injection risk,
 * and a missing-Javadoc public method.
 */
public class SampleVulnerableService {

    public String dbPassword = "SuperSecret123";

    public void processOrder(Customer customer, String orderId) {
        String name = customer.getName().toUpperCase();

        try {
            FileInputStream fis = new FileInputStream("/tmp/orders/" + orderId);
            fis.read();
        } catch (Exception e) {
            System.out.println("error");
        }
    }

    public void updateCustomerRecord(Connection conn, String customerId, String newEmail) throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE customers SET email = '" + newEmail + "' WHERE id = '" + customerId + "'");
    }

    static class Customer {
        private String name;
        public String getName() { return name; }
    }
}
