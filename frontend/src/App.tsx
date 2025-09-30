import React, { useState, useMemo } from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';

import LogUpload from './components/LogUpload.tsx';
import SectionFilter from './components/SectionFilter';
import StatsPanel from './components/StatsPanel';
import LogViewer from './components/LogViewer';
import { LogEntry, LogLevel, LogResponse } from './types/LogEntry';

const App: React.FC = () => {
  const [logData, setLogData] = useState<LogResponse | null>(null);
  const [selectedSection, setSelectedSection] = useState<string>('all');
  const [selectedLevel, setSelectedLevel] = useState<LogLevel | ''>('');

  const filteredEntries = useMemo(() => {
    if (!logData) return [];

    return logData.entries.filter(entry => {
      const sectionMatch = selectedSection === 'all' || entry.section === selectedSection;
      const levelMatch = !selectedLevel || entry.level === selectedLevel;
      return sectionMatch && levelMatch;
    });
  }, [logData, selectedSection, selectedLevel]);

  const handleLogsParsed = (data: LogResponse) => {
    console.log('Raw data received from backend:', data);
    console.log('Entries:', data.entries);
    console.log('Stats:', data.stats);
    setLogData(data);
    setSelectedSection('all');
    setSelectedLevel('');
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

              <SectionFilter
                section={selectedSection}
                level={selectedLevel}
                onSectionChange={setSelectedSection}
                onLevelChange={setSelectedLevel}
              />

              <LogViewer entries={filteredEntries} />
            </>
          )}
        </Col>
      </Row>
    </Container>
  );
};

export default App;