import React from 'react';
import { Card, Badge } from 'react-bootstrap';
import { LogEntry } from '../types/LogEntry';

interface TimelineEntry {
  tfReqId: string;
  resourceType?: string;
  startTime: string;
  endTime: string;
  durationMs: number;
  status: 'success' | 'error' | 'warning';
  entries: LogEntry[];
}

interface TimelineVisualizationProps {
  entries: LogEntry[];
  onEntryClick: (entry: TimelineEntry) => void;
}

const TimelineVisualization: React.FC<TimelineVisualizationProps> = ({
  entries,
  onEntryClick
}) => {
  // Группируем по tfReqId и строим временные линии
  const timelineEntries = React.useMemo(() => {
    const grouped = entries.reduce((acc, entry) => {
      if (entry.tfReqId) {
        if (!acc[entry.tfReqId]) {
          acc[entry.tfReqId] = [];
        }
        acc[entry.tfReqId].push(entry);
      }
      return acc;
    }, {} as Record<string, LogEntry[]>);

    return Object.entries(grouped).map(([tfReqId, groupEntries]) => {
      const timestamps = groupEntries
        .filter(e => e.timestamp)
        .map(e => new Date(e.timestamp).getTime());

      if (timestamps.length === 0) {
        return null;
      }

      const startTime = Math.min(...timestamps);
      const endTime = Math.max(...timestamps);

      // Определяем статус по наличию ошибок
      const hasErrors = groupEntries.some(e => e.level === 'ERROR');
      const hasWarnings = groupEntries.some(e => e.level === 'WARN');

      let status: 'success' | 'error' | 'warning' = 'success';
      if (hasErrors) status = 'error';
      else if (hasWarnings) status = 'warning';

      return {
        tfReqId,
        resourceType: groupEntries[0]?.tfResourceType,
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        durationMs: endTime - startTime,
        status,
        entries: groupEntries
      };
    }).filter(Boolean) as TimelineEntry[];
  }, [entries]);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'error': return '#dc2626';
      case 'warning': return '#d97706';
      default: return '#16a34a';
    }
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'error': return 'danger';
      case 'warning': return 'warning';
      default: return 'success';
    }
  };

  if (timelineEntries.length === 0) {
    return (
      <Card className="mt-4">
        <Card.Body>
          <div className="text-muted text-center">
            No request chains found for timeline visualization
          </div>
        </Card.Body>
      </Card>
    );
  }

  return (
    <Card className="mt-4">
      <Card.Header>
        <h5>Request Timeline</h5>
      </Card.Header>
      <Card.Body>
        <div className="timeline-container">
          {timelineEntries.map(entry => (
            <div
              key={entry.tfReqId}
              className="timeline-item mb-3 p-3 border rounded"
              style={{
                borderLeft: `4px solid ${getStatusColor(entry.status)}`,
                cursor: 'pointer'
              }}
              onClick={() => onEntryClick(entry)}
            >
              <div className="d-flex justify-content-between align-items-start">
                <div>
                  <strong>{entry.tfReqId}</strong>
                  {entry.resourceType && (
                    <span className="text-muted ms-2">({entry.resourceType})</span>
                  )}
                  <div className="text-muted small mt-1">
                    Duration: {entry.durationMs}ms •
                    Entries: {entry.entries.length}
                  </div>
                </div>
                <div>
                  <Badge bg={getStatusVariant(entry.status)}>
                    {entry.status}
                  </Badge>
                </div>
              </div>
              <div className="mt-2">
                <div className="timeline-bar bg-light rounded" style={{ height: '8px', position: 'relative' }}>
                  <div
                    className="h-100 rounded"
                    style={{
                      backgroundColor: getStatusColor(entry.status),
                      width: '100%'
                    }}
                  />
                </div>
                <div className="d-flex justify-content-between small text-muted mt-1">
                  <span>{new Date(entry.startTime).toLocaleTimeString()}</span>
                  <span>{new Date(entry.endTime).toLocaleTimeString()}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card.Body>
    </Card>
  );
};

export default TimelineVisualization;