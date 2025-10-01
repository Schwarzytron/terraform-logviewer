import React, { useState, useEffect } from 'react';
import { Form, Row, Col, Button, Card } from 'react-bootstrap';
import { LogEntry, SearchFilters } from '../types/LogEntry';
import { logApi } from '../services/logApi';

interface SearchPanelProps {
  logFileId: string;
  onSearch: (filters: SearchFilters, results: LogEntry[]) => void;
  onReset: () => void;
  availableResourceTypes: string[];
}

const SearchPanel: React.FC<SearchPanelProps> = ({
  logFileId,
  onSearch,
  onReset,
  availableResourceTypes
}) => {
  const [filters, setFilters] = useState<SearchFilters>({
    freeText: '',
    tfResourceType: '',
    level: undefined,
    section: '',
    tfReqId: '',
    onlyUnread: false,
    timestampFrom: undefined,
    timestampTo: undefined,
    page: 0,
    size: 50
  });

  const [isSearching, setIsSearching] = useState(false);

  const handleSearch = async () => {
    if (!logFileId) {
      console.error('No log file selected');
      return;
    }

    setIsSearching(true);
    try {
      const response = await logApi.advancedSearch(logFileId, filters);
      onSearch(filters, response.content); // Pass both filters and results
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      setIsSearching(false);
    }
  };

  const handleReset = () => {
    setFilters({
      freeText: '',
      tfResourceType: '',
      level: undefined,
      section: '',
      tfReqId: '',
      onlyUnread: false,
      timestampFrom: undefined,
      timestampTo: undefined,
      page: 0,
      size: 50
    });
    onReset();
  };

  // Instant search on free text change
  useEffect(() => {
    if (filters.freeText.length >= 2 || filters.freeText.length === 0) {
      const timeoutId = setTimeout(() => {
        if (logFileId) {
          handleSearch();
        }
      }, 300);
      return () => clearTimeout(timeoutId);
    }
  }, [filters.freeText, logFileId]);

  return (
    <Card className="mb-4">
      <Card.Header>
        <h5 className="mb-0">Advanced Search</h5>
      </Card.Header>
      <Card.Body>
        <Row className="g-3">
          <Col md={12}>
            <Form.Group>
              <Form.Label>Quick Search</Form.Label>
              <Form.Control
                type="text"
                placeholder="Search in messages and JSON..."
                value={filters.freeText}
                onChange={(e) => setFilters({ ...filters, freeText: e.target.value })}
                disabled={isSearching}
              />
            </Form.Group>
          </Col>

          <Col md={4}>
            <Form.Group>
              <Form.Label>Resource Type</Form.Label>
              <Form.Select
                value={filters.tfResourceType}
                onChange={(e) => setFilters({ ...filters, tfResourceType: e.target.value })}
              >
                <option value="">All Resources</option>
                {availableResourceTypes.map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>

          <Col md={4}>
            <Form.Group>
              <Form.Label>Request ID</Form.Label>
              <Form.Control
                type="text"
                placeholder="Filter by tf_req_id"
                value={filters.tfReqId}
                onChange={(e) => setFilters({ ...filters, tfReqId: e.target.value })}
              />
            </Form.Group>
          </Col>

          <Col md={4}>
            <Form.Group>
              <Form.Label>Section</Form.Label>
              <Form.Select
                value={filters.section}
                onChange={(e) => setFilters({ ...filters, section: e.target.value })}
              >
                <option value="">All Sections</option>
                <option value="plan">Plan</option>
                <option value="apply">Apply</option>
                <option value="other">Other</option>
              </Form.Select>
            </Form.Group>
          </Col>

          <Col md={12}>
            <Form.Check
              type="checkbox"
              label="Show only unread entries"
              checked={filters.onlyUnread || false}
              onChange={(e) => setFilters({ ...filters, onlyUnread: e.target.checked })}
            />
          </Col>

          <Col md={12}>
            <div className="d-flex gap-2">
              <Button variant="primary" onClick={handleSearch} disabled={isSearching || !logFileId}>
                {isSearching ? 'Searching...' : 'Apply Filters'}
              </Button>
              <Button variant="outline-secondary" onClick={handleReset} disabled={isSearching}>
                Reset
              </Button>
            </div>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
};

export default SearchPanel;