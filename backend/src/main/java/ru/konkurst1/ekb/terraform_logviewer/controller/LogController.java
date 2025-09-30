@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {
    
    @Autowired
    private LogParserService logParserService;
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadLogs(@RequestParam("file") MultipartFile file) {
        try {
            List<String> lines = Files.readAllLines(
                Paths.get(File.createTempFile("terraform-log", ".txt").getAbsolutePath())
            );
            
            List<LogEntry> entries = logParserService.parseLogs(lines);
            
            Map<String, Object> response = new HashMap<>();
            response.put("entries", entries);
            response.put("stats", calculateStats(entries));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/entries")
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(required = false) String section,
            @RequestParam(required = false) LogLevel level) {
        
        // TODO: реализовать фильтрацию из БД
        List<LogEntry> mockEntries = Arrays.asList(
            new LogEntry("Starting terraform plan", Instant.now(), LogLevel.INFO, "plan", "Starting terraform plan", false),
            new LogEntry("Using deprecated parameter", Instant.now(), LogLevel.WARN, "plan", "Using deprecated parameter", false)
        );
        
        return ResponseEntity.ok(mockEntries);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", 6);
        stats.put("planSectionEntries", 5);
        stats.put("applySectionEntries", 0);
        stats.put("errorEntries", 1);
        stats.put("warnEntries", 1);
        
        return ResponseEntity.ok(stats);
    }
    
    private Map<String, Object> calculateStats(List<LogEntry> entries) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", entries.size());
        stats.put("planSectionEntries", entries.stream().filter(e -> "plan".equals(e.getSection())).count());
        stats.put("applySectionEntries", entries.stream().filter(e -> "apply".equals(e.getSection())).count());
        stats.put("errorEntries", entries.stream().filter(e -> LogLevel.ERROR.equals(e.getLevel())).count());
        stats.put("warnEntries", entries.stream().filter(e -> LogLevel.WARN.equals(e.getLevel())).count());
        
        return stats;
    }
}