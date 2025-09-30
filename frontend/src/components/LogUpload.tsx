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
    if (!file) {
          console.log('No file selected');
          return;
        }
    console.log('File selected:', file.name, 'Size:', file.size, 'Type:', file.type);

    setIsUploading(true);
    setError('');

    const formData = new FormData();
    formData.append('file', file);

    try {
        console.log('Starting upload to:', 'http://localhost:8080/api/logs/upload');

      const response = await fetch('http://localhost:8080/api/logs/upload', {
        method: 'POST',
        body: formData,
      });

      console.log('Response status:', response.status);
      console.log('Response ok:', response.ok);

      if (!response.ok) {
              const errorText = await response.text();
              console.error('Upload failed with response:', errorText);
              throw new Error(`Upload failed: ${response.status} ${response.statusText}`);
            }

      const data = await response.json();

      console.log('Upload successful, received data:', data);
      console.log('Entries count:', data.entries?.length || 0);

      onLogsParsed(data);
    } catch (err) {
      console.error('Upload error:', err);
      setError('Failed to upload and parse logs');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="mb-4">
      <Form>
        <Form.Group>
          <Form.Control
            type="file"
            accept=".json,.log,.txt"
            onChange={handleFileUpload}
            disabled={isUploading}
          />
        </Form.Group>
      </Form>

      {isUploading && (
        <div className="mt-2">
          <Spinner animation="border" size="sm" /> Uploading and parsing logs...
        </div>
      )}

      {error && <Alert variant="danger" className="mt-2">{error}</Alert>}
    </div>
  );
};

export default LogUpload;