import React, { useState, useEffect } from 'react';
import { Card, Button, Form, Alert, Table, Badge } from 'react-bootstrap';

interface PluginInfo {
  name: string;
  version: string;
  description: string;
  supportedParameters: string[];
}

interface PluginManagerProps {
  logFileId: string;
}

const PluginManager: React.FC<PluginManagerProps> = ({ logFileId }) => {
  const [plugins, setPlugins] = useState<string[]>([]);
  const [pluginInfo, setPluginInfo] = useState<Record<string, PluginInfo>>({});
  const [results, setResults] = useState<Record<string, any>>({});
  const [newPlugin, setNewPlugin] = useState({ name: '', host: 'localhost', port: '50051' });
  const [parameters, setParameters] = useState<Record<string, string>>({});

  const loadPlugins = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/plugins/registered');
      const pluginList = await response.json();
      setPlugins(pluginList);

      // Загружаем информацию о каждом плагине
      for (const pluginName of pluginList) {
        const infoResponse = await fetch(`http://localhost:8080/api/plugins/${pluginName}/info`);
        if (infoResponse.ok) {
          const info = await infoResponse.json();
          setPluginInfo(prev => ({ ...prev, [pluginName]: info }));
        }
      }
    } catch (error) {
      console.error('Failed to load plugins:', error);
    }
  };

  const registerPlugin = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/plugins/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newPlugin)
      });

      if (response.ok) {
        await loadPlugins();
        setNewPlugin({ name: '', host: 'localhost', port: '50051' });
      }
    } catch (error) {
      console.error('Failed to register plugin:', error);
    }
  };

  const executePlugin = async (pluginName: string) => {
    try {
      const response = await fetch(`http://localhost:8080/api/plugins/${pluginName}/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          logFileId,
          parameters: parameters[pluginName] || {}
        })
      });

      const result = await response.json();
      setResults(prev => ({ ...prev, [pluginName]: result }));
    } catch (error) {
      console.error('Failed to execute plugin:', error);
    }
  };

  useEffect(() => {
    loadPlugins();
  }, []);

  return (
    <Card className="mt-4">
      <Card.Header>
        <h5>Plugin Manager</h5>
      </Card.Header>
      <Card.Body>
        {/* Регистрация плагина */}
        <div className="mb-4">
          <h6>Register New Plugin</h6>
          <div className="row g-2">
            <div className="col-md-3">
              <Form.Control
                placeholder="Plugin Name"
                value={newPlugin.name}
                onChange={(e) => setNewPlugin({...newPlugin, name: e.target.value})}
              />
            </div>
            <div className="col-md-3">
              <Form.Control
                placeholder="Host"
                value={newPlugin.host}
                onChange={(e) => setNewPlugin({...newPlugin, host: e.target.value})}
              />
            </div>
            <div className="col-md-2">
              <Form.Control
                placeholder="Port"
                value={newPlugin.port}
                onChange={(e) => setNewPlugin({...newPlugin, port: e.target.value})}
              />
            </div>
            <div className="col-md-2">
              <Button onClick={registerPlugin}>Register</Button>
            </div>
          </div>
        </div>

        {/* Список плагинов */}
        <div>
          <h6>Registered Plugins</h6>
          {plugins.length === 0 ? (
            <Alert variant="info">No plugins registered</Alert>
          ) : (
            plugins.map(pluginName => (
              <Card key={pluginName} className="mb-3">
                <Card.Body>
                  <div className="d-flex justify-content-between align-items-start">
                    <div>
                      <h6>{pluginName}</h6>
                      {pluginInfo[pluginName] && (
                        <div>
                          <Badge bg="secondary" className="me-2">
                            v{pluginInfo[pluginName].version}
                          </Badge>
                          <span className="text-muted">
                            {pluginInfo[pluginName].description}
                          </span>
                        </div>
                      )}
                    </div>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => executePlugin(pluginName)}
                    >
                      Execute
                    </Button>
                  </div>

                  {/* Результаты выполнения */}
                  {results[pluginName] && (
                    <div className="mt-3">
                      <h6>Results:</h6>
                      <pre className="bg-light p-2 rounded">
                        {JSON.stringify(results[pluginName], null, 2)}
                      </pre>
                    </div>
                  )}
                </Card.Body>
              </Card>
            ))
          )}
        </div>
      </Card.Body>
    </Card>
  );
};

export default PluginManager;