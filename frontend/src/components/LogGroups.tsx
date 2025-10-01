import React, { useState } from 'react';
import { Card, Badge, Button, Collapse } from 'react-bootstrap';
import { LogEntry } from '../types/LogEntry';

interface LogGroupsProps {
  entries: LogEntry[];
  onGroupClick: (tfReqId: string) => void;
  onMarkAsRead: (entryIds: number[]) => void;
}

const LogGroups: React.FC<LogGroupsProps> = ({ entries, onGroupClick, onMarkAsRead }) => {
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());

  // Group entries by tf_req_id
  const groupedEntries = entries.reduce((acc, entry) => {
    if (entry.tfReqId) {
      if (!acc[entry.tfReqId]) {
        acc[entry.tfReqId] = [];
      }
      acc[entry.tfReqId].push(entry);
    }
    return acc;
  }, {} as Record<string, LogEntry[]>);

  const toggleGroup = (tfReqId: string) => {
    const newExpanded = new Set(expandedGroups);
    if (newExpanded.has(tfReqId)) {
      newExpanded.delete(tfReqId);
    } else {
      newExpanded.add(tfReqId);
    }
    setExpandedGroups(newExpanded);
  };

  const handleMarkGroupAsRead = (groupEntries: LogEntry[]) => {
    const unreadIds = groupEntries
      .filter(entry => !entry.isRead)
      .map(entry => entry.id);
    if (unreadIds.length > 0) {
      onMarkAsRead(unreadIds);
    }
  };

  if (Object.keys(groupedEntries).length === 0) {
    return <div className="text-muted">No grouped entries found</div>;
  }

  return (
    <div className="log-groups">
      {Object.entries(groupedEntries).map(([tfReqId, groupEntries]) => (
        <Card key={tfReqId} className="mb-3 request-chain">
          <Card.Header
            className="chain-header"
            onClick={() => toggleGroup(tfReqId)}
            style={{ cursor: 'pointer' }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <div>
                <strong>Request: {tfReqId}</strong>
                <Badge bg="secondary" className="ms-2">
                  {groupEntries.length} entries
                </Badge>
                <Badge bg="info" className="ms-1">
                  {groupEntries.filter(e => !e.isRead).length} unread
                </Badge>
              </div>
              <div>
                <Button
                  variant="outline-primary"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleMarkGroupAsRead(groupEntries);
                  }}
                >
                  Mark Read
                </Button>
              </div>
            </div>
          </Card.Header>

          <Collapse in={expandedGroups.has(tfReqId)}>
            <div>
              <Card.Body>
                {groupEntries.map(entry => (
                  <div
                    key={entry.id}
                    className={`log-entry mb-2 p-2 ${entry.isRead ? 'read-entry' : 'unread-entry'}`}
                    onClick={() => onGroupClick(entry.tfReqId!)}
                  >
                    <div className="d-flex justify-content-between">
                      <span>
                        <Badge bg={getLevelVariant(entry.level)} className="me-2">
                          {entry.level}
                        </Badge>
                        {entry.message}
                      </span>
                      <small className="text-muted">
                        {new Date(entry.timestamp).toLocaleTimeString()}
                      </small>
                    </div>
                  </div>
                ))}
              </Card.Body>
            </div>
          </Collapse>
        </Card>
      ))}
    </div>
  );
};

// Helper function (you might want to move this to a utils file)
const getLevelVariant = (level: string): string => {
  switch (level) {
    case 'ERROR': return 'danger';
    case 'WARN': return 'warning';
    case 'INFO': return 'info';
    case 'DEBUG': return 'secondary';
    default: return 'dark';
  }
};

export default LogGroups;