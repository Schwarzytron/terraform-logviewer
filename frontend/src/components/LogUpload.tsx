import React, { useState } from 'react';
import { Button, Form, Alert, Spinner } from 'react-bootstrap';

interface LogUploadProps {
  onLogsParsed: (data: any) => void;
}

const LogUpload: React.FC<LogUploadProps> = ({ onLogsParsed }) => {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string>('');

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setIsUploading(true);
    setError('');

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('http://localhost:8080/api/logs/upload', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) throw new Error('Upload failed');

      const data = await response.json();
      onLogsParsed(data);
    } catch (err) {
      setError('Failed to upload and parse logs');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="mb-4">
      <h3>Upload Terraform Logs</h3>
      <Form>
        <Form.Group>
          <Form.Label>Select log file:</Form.Label>
          <Form.Control
            type="file"
            accept=".log,.txt"
            onChange={handleFileUpload}
            disabled={isUploading}
          />
        </Form.Group>
      </Form>

      {isUploading && (
        <div className="mt-2">
          <Spinner animation="border" size="sm" /> Parsing logs...
        </div>
      )}

      {error && <Alert variant="danger" className="mt-2">{error}</Alert>}
    </div>
  );
};

export default LogUpload;