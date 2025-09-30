import React from 'react';
import { Table, Badge } from 'react-bootstrap';
import { LogEntry, LogLevel } from '../types/LogEntry';

interface LogViewerProps {
  entries: LogEntry[];
}

const LogViewer: React.FC<LogViewerProps> = ({ entries }) => {
  const getLevelVariant = (level: LogLevel) => {
    switch (level) {
      case LogLevel.ERROR: return 'danger';
      case LogLevel.WARN: return 'warning';
      case LogLevel.INFO: return 'info';
      case LogLevel.DEBUG: return 'secondary';
      default: return 'light';
    }
  };

  const getSectionVariant = (section: string) => {
    switch (section) {
      case 'plan': return 'primary';
      case 'apply': return 'success';
      default: return 'secondary';
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  return (
    <div>
      <h4>Parsed Log Entries ({entries.length})</h4>

      <Table striped bordered hover responsive>
        <thead>
          <tr>
            <th>Time</th>
            <th>Level</th>
            <th>Section</th>
            <th>Message</th>
            <th>JSON</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.id}>
              <td className="text-nowrap">
                {formatTimestamp(entry.timestamp)}
              </td>
              <td>
                <Badge bg={getLevelVariant(entry.level)}>
                  {entry.level}
                </Badge>
              </td>
              <td>
                <Badge bg={getSectionVariant(entry.section)}>
                  {entry.section}
                </Badge>
              </td>
              <td style={{ maxWidth: '500px' }}>
                <div>
                  <strong>{entry.message}</strong>
                  {entry.rawMessage !== entry.message && (
                    <div className="text-muted small mt-1">
                      {entry.rawMessage}
                    </div>
                  )}
                </div>
              </td>
              <td>
                {entry.hasJson && (
                  <Badge bg="info">JSON</Badge>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </Table>

      {entries.length === 0 && (
        <div className="text-center text-muted py-4">
          No log entries to display. Upload a Terraform log file to get started.
        </div>
      )}
    </div>
  );
};

export default LogViewer;