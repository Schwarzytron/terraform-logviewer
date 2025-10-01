import React, { useState, useMemo } from 'react';
import { Container, Row, Col, Button, Navbar, Nav } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';
import LogUpload from './components/LogUpload.tsx';
import SearchPanel from './components/SearchPanel.tsx';
import SectionFilter from './components/SectionFilter';
import StatsPanel from './components/StatsPanel';
import LogViewer from './components/LogViewer';
import TimelineVisualization from './components/TimelineVisualization';
import ThemeToggle from './components/ThemeToggle';
import { ThemeProvider } from './contexts/ThemeContext';
import PluginManager from './components/PluginManager';
import { LogEntry, LogLevel, LogResponse, SearchFilters } from './types/LogEntry';

const AppContent: React.FC = () => {
  const [logData, setLogData] = useState<LogResponse | null>(null);
  const [selectedSection, setSelectedSection] = useState<string>('all');
  const [selectedLevel, setSelectedLevel] = useState<LogLevel | ''>('');
  const [searchResults, setSearchResults] = useState<LogEntry[]>([]);
  const [isSearchActive, setIsSearchActive] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const displayedEntries = useMemo(() => {
    const entries = isSearchActive ? searchResults : (logData?.entries || []);

    return entries.filter(entry => {
      const sectionMatch = selectedSection === 'all' || entry.section === selectedSection;
      const levelMatch = !selectedLevel || entry.level === selectedLevel;
      return sectionMatch && levelMatch;
    });
  }, [logData, selectedSection, selectedLevel, searchResults, isSearchActive]);

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    loadPage(page);
  };

  const loadPage = async (page: number) => {
    if (!logData?.logFileId) return;

    try {
      const response = await fetch(
        `http://localhost:8080/api/logs/entries?logFileId=${logData.logFileId}&page=${page}&size=50`
      );
      if (response.ok) {
        const data = await response.json();
        setCurrentPage(data.currentPage);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
        // Обновляем отображаемые записи
        setLogData(prev => prev ? { ...prev, entries: data.content } : null);
      }
    } catch (error) {
      console.error('Failed to load page:', error);
    }
  };

  const handleLogsParsed = (data: LogResponse) => {
    console.log('Raw data received from backend:', data);
    setLogData(data);
    setSearchResults([]);
    setIsSearchActive(false);
    setSelectedSection('all');
    setSelectedLevel('');
    setCurrentPage(0);
  };

  const handleSearch = (filters: SearchFilters, results: LogEntry[]) => {
    setSearchResults(results);
    setIsSearchActive(true);
  };

  const handleSearchReset = () => {
    setSearchResults([]);
    setIsSearchActive(false);
  };

  const handleTimelineClick = (entry: any) => {
    console.log('Timeline entry clicked:', entry);
    // Можно добавить функционал для показа деталей цепочки запросов
  };

  return (
    <>
      <Navbar bg="body" className="border-bottom">
        <Container>
          <Navbar.Brand>
            <h1 className="h4 mb-0">Terraform Log Viewer</h1>
          </Navbar.Brand>
          <Nav className="ms-auto">
            <ThemeToggle />
          </Nav>
        </Container>
      </Navbar>
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
                <LogViewer
                  entries={displayedEntries}
                  pagination={
                    isSearchActive ? undefined : {
                      currentPage,
                      totalPages,
                      totalElements,
                      onPageChange: handlePageChange
                    }
                  }
                />
                <TimelineVisualization
                  entries={logData.entries}
                  onEntryClick={handleTimelineClick}
                />
                <PluginManager logFileId={logData.logFileId} />
              </>
            )}
          </Col>
        </Row>
      </Container>
    </>
  );
};

const App: React.FC = () => {
  return (
    <ThemeProvider>
      <AppContent />
    </ThemeProvider>
  );
};

export default App;