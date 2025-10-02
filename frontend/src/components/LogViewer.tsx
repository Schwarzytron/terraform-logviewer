import React from 'react';
import { Table, Badge, Alert, Button } from 'react-bootstrap';
import { LogEntry, LogLevel } from '../types/LogEntry';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}

interface LogViewerProps {
  entries: LogEntry[];
  pagination?: PaginationProps;
}

const PaginationComponent: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalElements,
  onPageChange
}) => {
  return (
    <div className="d-flex justify-content-between align-items-center mt-3">
      <div className="text-muted">
        Page {currentPage + 1} of {totalPages} ({totalElements} total entries)
      </div>
      <div>
        <Button
          variant="outline-primary"
          size="sm"
          disabled={currentPage === 0}
          onClick={() => onPageChange(currentPage - 1)}
        >
          Previous
        </Button>
        <span className="mx-2">|</span>
        <Button
          variant="outline-primary"
          size="sm"
          disabled={currentPage >= totalPages - 1}
          onClick={() => onPageChange(currentPage + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  );
};

const formatTimestamp = (timestamp: string): string => {
  if (!timestamp) return 'N/A';
  try {
    return new Date(timestamp).toLocaleString();
  } catch (e) {
    return timestamp;
  }
};

const getLevelVariant = (level: LogLevel): string => {
  switch (level) {
    case LogLevel.ERROR: return 'danger';
    case LogLevel.WARN: return 'warning';
    case LogLevel.INFO: return 'info';
    case LogLevel.DEBUG: return 'secondary';
    default: return 'dark';
  }
};

const getSectionVariant = (section: string): string => {
  switch (section) {
    case 'plan': return 'primary';
    case 'apply': return 'success';
    default: return 'secondary';
  }
};

const LogViewer: React.FC<LogViewerProps> = ({ entries, pagination }) => {
  console.log('LogViewer rendering with entries:', entries?.length || 0);

  const getRowClass = (entry: LogEntry) => {
    if (entry.parsingError) return 'table-danger';
    switch (entry.level) {
      case LogLevel.ERROR: return 'table-danger';
      case LogLevel.WARN: return 'table-warning';
      default: return '';
    }
  };

  if (!entries || entries.length === 0) {
    return (
      <Alert variant="info" className="mt-3">
        No log entries to display
      </Alert>
    );
  }

  return (
    <div className="mt-3">
      <div className="mb-2 text-muted">
        Showing {entries.length} log entries
        {pagination && ` (page ${pagination.currentPage + 1} of ${pagination.totalPages})`}
      </div>

      <Table striped bordered hover responsive>
        <thead>
          <tr>
            <th>#</th>
            <th>Time</th>
            <th>Level</th>
            <th>Section</th>
            <th>Message</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.id} className={getRowClass(entry)}>
              <td>{entry.lineNumber}</td>
              <td className="text-nowrap">
                {formatTimestamp(entry.timestamp)}
                {!entry.timestamp && (
                  <Badge bg="secondary" className="ms-1">No TS</Badge>
                )}
              </td>
              <td>
                <Badge bg={getLevelVariant(entry.level)}>
                  {entry.level}
                </Badge>
                {!entry.level && (
                  <Badge bg="dark" className="ms-1">No Level</Badge>
                )}
              </td>
              <td>
                <Badge bg={getSectionVariant(entry.section)}>
                  {entry.section}
                </Badge>
              </td>
              <td style={{ maxWidth: '500px' }}>
                <div>
                  {entry.parsingError ? (
                    <div>
                      <Alert variant="danger" className="py-1 mb-1">
                        <strong>Parse Error:</strong> {entry.parsingErrorMessage}
                      </Alert>
                      <code className="text-muted">{entry.rawMessage}</code>
                    </div>
                  ) : (
                    <>
                      <strong>{entry.message}</strong>
                      {entry.hasJson && (
                        <Badge bg="info" className="ms-1">JSON</Badge>
                      )}
                    </>
                  )}
                </div>
              </td>
              <td>
                {entry.parsingError && (
                  <Badge bg="danger">Parse Error</Badge>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </Table>

      {pagination && (
        <PaginationComponent
          currentPage={pagination.currentPage}
          totalPages={pagination.totalPages}
          totalElements={pagination.totalElements}
          onPageChange={pagination.onPageChange}
        />
      )}
    </div>
  );
};

export default LogViewer;