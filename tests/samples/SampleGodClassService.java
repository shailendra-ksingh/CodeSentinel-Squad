/**
 * A deliberately oversized "God Class" sample used to demonstrate the
 * ArchitectureAgent, which only dispatches when StaticAnalyzer's design-smell
 * heuristic (too many public methods) fires. Run CodeSentinel against this
 * file alongside SampleVulnerableService.java to show both the Security/Test
 * path AND the Architecture path dispatching in one demo.
 */
public class SampleGodClassService {

    public void createOrder() { }
    public void updateOrder() { }
    public void deleteOrder() { }
    public void createInvoice() { }
    public void updateInvoice() { }
    public void sendEmailNotification() { }
    public void sendSmsNotification() { }
    public void generateReport() { }
    public void archiveOldRecords() { }
    public void syncWithExternalSystem() { }
}
