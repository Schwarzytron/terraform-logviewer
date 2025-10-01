import React, { useState, useMemo } from 'react';
import { Container, Row, Col, Button } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';
import LogUpload from './components/LogUpload.tsx';
import SearchPanel from './components/SearchPanel.tsx';
import SectionFilter from './components/SectionFilter';
import StatsPanel from './components/StatsPanel';
import LogViewer from './components/LogViewer';
import { LogEntry, LogLevel, LogResponse, SearchFilters } from './types/LogEntry';

const App: React.FC = () => {
  const [logData, setLogData] = useState<LogResponse | null>(null);
  const [selectedSection, setSelectedSection] = useState<string>('all');
  const [selectedLevel, setSelectedLevel] = useState<LogLevel | ''>('');
  const [searchResults, setSearchResults] = useState<LogEntry[]>([]);
  const [isSearchActive, setIsSearchActive] = useState(false);

  const displayedEntries = useMemo(() => {
    const entries = isSearchActive ? searchResults : (logData?.entries || []);

    return entries.filter(entry => {
      const sectionMatch = selectedSection === 'all' || entry.section === selectedSection;
      const levelMatch = !selectedLevel || entry.level === selectedLevel;
      return sectionMatch && levelMatch;
    });
  }, [logData, selectedSection, selectedLevel, searchResults, isSearchActive]);

  const handleLogsParsed = (data: LogResponse) => {
    console.log('Raw data received from backend:', data);
    setLogData(data);
    setSearchResults([]);
    setIsSearchActive(false);
    setSelectedSection('all');
    setSelectedLevel('');
  };

  const handleSearch = (filters: SearchFilters, results: LogEntry[]) => {
    setSearchResults(results);
    setIsSearchActive(true);
  };

  const handleSearchReset = () => {
    setSearchResults([]);
    setIsSearchActive(false);
  };

  return (
    <Container fluid className="py-4">
      <Row>
        <Col>
          <h1 className="text-center mb-4">Terraform Log Viewer</h1>

          <LogUpload onLogsParsed={handleLogsParsed} />

          {logData && (
            <>
              <StatsPanel stats={logData.stats} />
              <SearchPanel
                logFileId={logData.logFileId}
                onSearch={handleSearch}
                onReset={handleSearchReset}
                availableResourceTypes={[]}
              />
              <SectionFilter
                section={selectedSection}
                level={selectedLevel}
                onSectionChange={setSelectedSection}
                onLevelChange={setSelectedLevel}
              />
              {isSearchActive && (
                <div className="alert alert-info mb-3">
                  Showing {searchResults.length} search results
                  <Button
                    variant="outline-info"
                    size="sm"
                    className="ms-2"
                    onClick={handleSearchReset}
                  >
                    Show All Entries
                  </Button>
                </div>
              )}
              <LogViewer entries={displayedEntries} />
            </>
          )}
        </Col>
      </Row>
    </Container>
  );
};

export default App;