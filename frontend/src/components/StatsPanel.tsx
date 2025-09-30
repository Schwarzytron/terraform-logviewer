import React from 'react';
import { Card, Row, Col } from 'react-bootstrap';
import { ParsingStats } from '../types/LogEntry';

interface StatsPanelProps {
  stats: ParsingStats;
}

const StatsPanel: React.FC<StatsPanelProps> = ({ stats }) => {
  return (
    <Card className="mb-4">
      <Card.Header>
        <h5 className="mb-0">Parsing Statistics</h5>
      </Card.Header>
      <Card.Body>
        <Row>
          <Col md={3} className="text-center">
            <div className="h4 text-primary mb-1">{stats.totalEntries}</div>
            <div className="text-muted">Total Entries</div>
          </Col>
          <Col md={2} className="text-center">
            <div className="h4 text-info mb-1">{stats.planSectionEntries}</div>
            <div className="text-muted">Plan</div>
          </Col>
          <Col md={2} className="text-center">
            <div className="h4 text-success mb-1">{stats.applySectionEntries}</div>
            <div className="text-muted">Apply</div>
          </Col>
          <Col md={2} className="text-center">
            <div className="h4 text-danger mb-1">{stats.errorEntries}</div>
            <div className="text-muted">Errors</div>
          </Col>
          <Col md={3} className="text-center">
            <div className="h4 text-warning mb-1">{stats.warnEntries}</div>
            <div className="text-muted">Warnings</div>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
};

export default StatsPanel;