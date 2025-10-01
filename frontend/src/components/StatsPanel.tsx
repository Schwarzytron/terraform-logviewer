import React from 'react';
import { Card, Row, Col, Badge } from 'react-bootstrap';

interface StatsPanelProps {
  stats: Record<string, any>;
}

const StatsPanel: React.FC<StatsPanelProps> = ({ stats }) => {
  return (
    <Card className="mb-4">
      <Card.Body>
        <Row className="text-center">
          <Col>
            <h6>Total Entries</h6>
            <Badge bg="primary" className="fs-6">
              {stats.totalEntries || 0}
            </Badge>
          </Col>
          <Col>
            <h6>Plan Section</h6>
            <Badge bg="info" className="fs-6">
              {stats.planSectionEntries || 0}
            </Badge>
          </Col>
          <Col>
            <h6>Apply Section</h6>
            <Badge bg="success" className="fs-6">
              {stats.applySectionEntries || 0}
            </Badge>
          </Col>
          <Col>
            <h6>Errors</h6>
            <Badge bg="danger" className="fs-6">
              {stats.errorEntries || 0}
            </Badge>
          </Col>
          <Col>
            <h6>Warnings</h6>
            <Badge bg="warning" className="fs-6">
              {stats.warnEntries || 0}
            </Badge>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
};

export default StatsPanel;