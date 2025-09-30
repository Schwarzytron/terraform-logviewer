import React from 'react';
import { Form } from 'react-bootstrap';
import { LogLevel } from '../types/LogEntry';

interface SectionFilterProps {
  section: string;
  level: LogLevel | '';
  onSectionChange: (section: string) => void;
  onLevelChange: (level: LogLevel | '') => void;
}

const SectionFilter: React.FC<SectionFilterProps> = ({
  section,
  level,
  onSectionChange,
  onLevelChange
}) => {
  return (
    <div className="mb-3 d-flex gap-3">
      <Form.Group>
        <Form.Label>Section:</Form.Label>
        <Form.Select
          value={section}
          onChange={(e) => onSectionChange(e.target.value)}
        >
          <option value="all">All Sections</option>
          <option value="plan">Plan</option>
          <option value="apply">Apply</option>
          <option value="other">Other</option>
        </Form.Select>
      </Form.Group>

      <Form.Group>
        <Form.Label>Level:</Form.Label>
        <Form.Select
          value={level}
          onChange={(e) => onLevelChange(e.target.value as LogLevel | '')}
        >
          <option value="">All Levels</option>
          <option value={LogLevel.ERROR}>Error</option>
          <option value={LogLevel.WARN}>Warning</option>
          <option value={LogLevel.INFO}>Info</option>
          <option value={LogLevel.DEBUG}>Debug</option>
        </Form.Select>
      </Form.Group>
    </div>
  );
};

export default SectionFilter;