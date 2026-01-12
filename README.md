# CodeovAi - Advanced Code Analysis System

A robust, modular backend system for analyzing uploaded backend projects (ZIP files) and diagram files (PDF/IMAGE) using AI-powered analysis.

## Features

### 🔍 **Input Classification**
- **ZIP Archives**: Backend project source code analysis
- **PDF Documents**: UML diagram and text extraction
- **Image Files**: OCR-based diagram interpretation
- **Text Files**: Future specification analysis support

### 🛡️ **Security & Robustness**
- File signature validation (not just extensions)
- Path traversal attack prevention
- Malicious file detection and filtering
- Size limits and bomb protection
- Secure temporary file handling

### 🧠 **Advanced Analysis**
- **AST-based Code Parsing**: Java, Kotlin, TypeScript, PHP support
- **Intelligent Diagram Analysis**: OCR with relationship detection
- **Dependency Graph Extraction**: Understand system architecture
- **Framework Detection**: Spring, NestJS, Laravel recognition
- **Caching System**: Redis-based performance optimization

### 📊 **AI Integration**
- **Structured Prompt Building**: Consistent, comprehensive prompts
- **Gemini API Integration**: Advanced AI analysis
- **Context-Aware Responses**: System-specific explanations
- **Risk Assessment**: Security and performance insights

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Controller    │───▶│ InputClassifier  │───▶│   Pipeline      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                       ┌─────────────────┐             │
                       │   AI Service    │◀────────────┘
                       └─────────────────┘
                                │
                       ┌─────────────────┐
                       │   Cache & Log  │
                       └─────────────────┘
```

### Core Services

- **InputClassifierService**: File type detection and routing
- **SecureZipExtractionService**: Safe ZIP processing
- **AstCodeScanningService**: Advanced code analysis
- **VisualInterpretationService**: OCR and diagram parsing
- **EnhancedPromptBuilderService**: Structured AI prompts
- **CacheService**: Performance optimization
- **LoggingService**: Structured monitoring

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Redis server
- Tesseract OCR (for visual analysis)
- Gemini API key

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd codeovai
   ```

2. **Install Tesseract OCR**
   ```bash
   # Ubuntu/Debian
   sudo apt-get install tesseract-ocr
   
   # macOS
   brew install tesseract
   
   # Windows
   # Download from https://github.com/UB-Mannheim/tesseract/wiki
   ```

3. **Download Tesseract Data**
   ```bash
   mkdir -p tessdata
   cd tessdata
   wget https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
   cd ..
   ```

4. **Configure Redis**
   ```bash
   # Start Redis server
   redis-server
   ```

5. **Set up Environment Variables**
   ```bash
   export GEMINI_API_KEY=your-gemini-api-key-here
   ```

6. **Build and Run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

### Configuration

Edit `src/main/resources/application.yml`:

```yaml
codeovai:
  gemini:
    api-key: ${GEMINI_API_KEY:your-gemini-api-key-here}
    model: gemini-pro
    max-tokens: 4096
    temperature: 0.3
  
  ocr:
    tessdata-path: tessdata
    language: eng
  
  max-zip-size: 100MB
  max-entry-size: 10MB
  max-entries: 1000
```

## API Usage

### Analyze Files

```bash
curl -X POST \
  http://localhost:8080/api/codeovAi \
  -F "files=@project.zip" \
  -F "files=@diagram.png"
```

### Response Format

```json
{
  "fileName": "project.zip",
  "inputType": "ZIP_ARCHIVE",
  "codeElements": [
    {
      "name": "UserController",
      "elementType": "CLASS",
      "sourceType": "JAVA",
      "annotations": ["@RestController", "@RequestMapping"],
      "methods": ["getAllUsers()", "getUserById()"],
      "filePath": "src/main/java/com/example/UserController.java"
    }
  ],
  "systemContext": {
    "systemPurpose": "Web application with REST API controllers",
    "architectureOverview": "Three-tier architecture with Spring Boot",
    "coreFlow": {
      "mainComponents": "UserController, UserService, UserRepository",
      "dataFlow": "Request → Controller → Service → Repository → Database",
      "controlFlow": "Unknown - Requires runtime analysis"
    },
    "assumptions": ["System uses database persistence", "System provides REST API endpoints"],
    "unknowns": ["Database schema details", "Security configuration"]
  },
  "explanation": "This is a Spring Boot web application..."
}
```

### Health Check

```bash
curl http://localhost:8080/api/codeovAi/health
```

### Clear Cache

```bash
curl -X DELETE http://localhost:8080/api/codeovAi/cache
```

## Supported Languages

### Code Analysis
- ✅ Java (full AST parsing)
- ✅ Kotlin (basic parsing)
- ✅ TypeScript (basic parsing)
- ✅ JavaScript (basic parsing)
- ✅ PHP (basic parsing)

### Diagram Analysis
- ✅ UML Class Diagrams
- ✅ Component Diagrams
- ✅ Flowcharts
- ✅ Architecture Diagrams
- ✅ Multi-page PDFs

## Security Features

### File Validation
- Signature-based file type detection
- Size limits (configurable)
- Path traversal prevention
- Dangerous file filtering

### Processing Security
- Isolated extraction directories
- Automatic cleanup
- Malicious pattern detection
- Resource usage limits

## Performance Optimization

### Caching Strategy
- **Multi-level caching**: In-memory + Redis
- **Smart TTL**: Based on file type and complexity
- **Cache warming**: Common patterns pre-loaded
- **Automatic cleanup**: Expired entry removal

### Processing Optimization
- **Async processing**: Non-blocking file analysis
- **Parallel scanning**: Multi-threaded code analysis
- **Stream processing**: Large file handling
- **Resource pooling**: Reuse expensive resources

## Monitoring & Logging

### Structured Logging
```json
{
  "timestamp": "2024-01-12T10:30:00Z",
  "pipeline": "ZIP_EXTRACTION",
  "message": "Starting secure ZIP extraction",
  "filename": "project.zip",
  "size": "1048576",
  "level": "INFO"
}
```

### Performance Metrics
- Processing time per pipeline
- File size and complexity metrics
- Cache hit/miss ratios
- Error rates and types

### Security Events
- Path traversal attempts
- Malicious file detection
- Size limit violations
- Suspicious pattern matches

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn test -Dtest=**/*IntegrationTest
```

### Test Coverage
```bash
mvn jacoco:report
```

## Development

### Code Structure
```
src/main/java/codeovai/codeovai/
├── controller/           # REST endpoints
├── service/             # Business logic
│   ├── ai/             # AI integration
│   ├── analyze/        # Code analysis
│   ├── classification/ # File type detection
│   ├── core/           # Core services
│   ├── upload/         # File handling
│   └── visual/         # Diagram analysis
├── model/              # Data models
└── enumeration/        # Enums and constants
```

### Adding New Features

1. **New File Type**: Add to `InputType` enum and `InputClassifierService`
2. **New Language**: Extend `AstCodeScanningService`
3. **New AI Provider**: Implement AI service interface
4. **New Analysis**: Add to context builder and prompt service

## Production Deployment

### Docker Support
```dockerfile
FROM openjdk:17-jre-slim
COPY target/codeovai-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables
- `GEMINI_API_KEY`: Required for AI analysis
- `REDIS_HOST`: Redis server host
- `REDIS_PORT`: Redis server port
- `TESSDATA_PREFIX`: Tesseract data directory

### Monitoring
- Spring Actuator endpoints: `/actuator/health`, `/actuator/metrics`
- Custom metrics: `/actuator/prometheus`
- Structured logs: JSON format with correlation IDs

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue on GitHub
- Check the documentation
- Review existing issues

## Roadmap

### Upcoming Features
- [ ] Real-time analysis streaming
- [ ] Additional AI model support
- [ ] Advanced dependency analysis
- [ ] Code quality metrics
- [ ] Security vulnerability scanning
- [ ] Performance profiling
- [ ] Multi-language support in UI
- [ ] Batch processing capabilities

### Enhancements
- [ ] Kubernetes deployment guides
- [ ] Advanced caching strategies
- [ ] Custom analysis rules
- [ ] Integration with CI/CD pipelines
- [ ] Webhook support
- [ ] Analysis history and comparison
