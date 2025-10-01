import React, { useState } from 'react';
import { Card, Button, Collapse } from 'react-bootstrap';

interface JsonViewerProps {
  json: string;
  isExpanded: boolean;
  onToggle: () => void;
}

const JsonViewer: React.FC<JsonViewerProps> = ({ json, isExpanded, onToggle }) => {
  const [expandedPaths, setExpandedPaths] = useState<Set<string>>(new Set());

  const formatJson = (jsonString: string): string => {
    try {
      const parsed = JSON.parse(jsonString);
      return JSON.stringify(parsed, null, 2);
    } catch (e) {
      return jsonString;
    }
  };

  const togglePath = (path: string) => {
    const newExpanded = new Set(expandedPaths);
    if (newExpanded.has(path)) {
      newExpanded.delete(path);
    } else {
      newExpanded.add(path);
    }
    setExpandedPaths(newExpanded);
  };

  const renderJsonValue = (value: any, path: string = ''): React.ReactNode => {
    if (typeof value === 'object' && value !== null) {
      const isExpanded = expandedPaths.has(path);
      return (
        <div>
          <span
            className="json-toggle"
            onClick={() => togglePath(path)}
            style={{ cursor: 'pointer', color: '#007bff' }}
          >
            {isExpanded ? '▼' : '►'} {Array.isArray(value) ? 'Array' : 'Object'}
          </span>
          <Collapse in={isExpanded}>
            <div style={{ marginLeft: '20px' }}>
              {Object.entries(value).map(([key, val]) => (
                <div key={key}>
                  <span style={{ color: '#881391' }}>"{key}"</span>: {renderJsonValue(val, `${path}.${key}`)}
                </div>
              ))}
            </div>
          </Collapse>
        </div>
      );
    }
    return <span style={{ color: typeof value === 'string' ? '#c41a16' : '#1c00cf' }}>
      {typeof value === 'string' ? `"${value}"` : String(value)}
    </span>;
  };

  return (
    <Card className="mt-2">
      <Card.Header className="py-2">
        <Button
          variant="link"
          onClick={onToggle}
          className="p-0 text-decoration-none"
        >
          {isExpanded ? '▼' : '►'} JSON Data
        </Button>
      </Card.Header>

      <Collapse in={isExpanded}>
        <Card.Body className="py-2">
          <pre style={{ fontSize: '0.8rem', margin: 0, maxHeight: '400px', overflow: 'auto' }}>
            {renderJsonValue(JSON.parse(json))}
          </pre>
        </Card.Body>
      </Collapse>
    </Card>
  );
};

export default JsonViewer;