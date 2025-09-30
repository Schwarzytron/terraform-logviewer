@Entity
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String rawMessage;

    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    private LogLevel level;

    private String section; // "plan", "apply", "other"

    @Column(columnDefinition = "TEXT")
    private String message;

    private Boolean hasJson;

    public LogEntry() {}

    public LogEntry(String rawMessage, Instant timestamp, LogLevel level,
                    String section, String message, Boolean hasJson) {
        this.rawMessage = rawMessage;
        this.timestamp = timestamp;
        this.level = level;
        this.section = section;
        this.message = message;
        this.hasJson = hasJson;
    }
}