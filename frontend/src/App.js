import React, { useState, useEffect } from 'react';
import { Container, Table, Form } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';

function App() {
  const [logs, setLogs] = useState([]);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    fetch('/api/logs')
      .then(response => response.json())
      .then(data => setLogs(data));
  }, []);

  return (
  <Container className="mt-4">
  <h1>Terraform Log Viewer</h1>
  <Form.Group className="mb-3">
  <Form.Control
  type="text"
  placeholder="Filter logs..."
  value={filter}
  onChange={(e) => setFilter(e.target.value)}
  />
  </Form.Group>
  <Table striped bordered hover>
  <thead>
  <tr>
  <th>Timestamp</th>
  <th>Level</th>
  <th>Message</th>
  </tr>
  </thead>
  <tbody>
  {logs.map((log, index) => (
  <tr key={index}>
  <td>{log.timestamp}</td>
  <td>{log.level}</td>
  <td>{log.message}</td>
  </tr>
  ))}
</tbody>
</Table>
</Container>
);
}

export default App;