# HTTP Server Class Diagram

```mermaid
%%{
  init: {
    'theme': 'base',
    'themeVariables': {
      'fontSize': '16px',
      'fontFamily': 'arial',
      'lineHeight': '1.4',
      'fontWeight': 'normal'
    }
  }
}%%

classDiagram
    direction TB
    %% Set diagram width and height
    %% size 1600x1200

    class App {
        +main()
    }
    
    class SimpleWebServer {
        -ExecutorService executor
        -boolean running
        +start()
        -handleRequest(Socket)
        -handleAdminRequest(HttpRequest, HttpResponse)
        -handleGet(HttpRequest, HttpResponse)
        -serveFile(File, HttpResponse)
        -sendError(HttpResponse, int, String)
    }
    
    class HttpRequest {
        -String method
        -String path
        -String version
        -Map headers
        -Map queryParams
        -Map formParams
        -Map cookies
        -String body
        +getMethod()
        +getPath()
        +getVersion()
        +getHeaders()
        +getBody()
        +getCookie(String)
        +getQueryParam(String)
        +getFormParam(String)
    }
    
    class HttpResponse {
        -int statusCode
        -String statusMessage
        -Map headers
        -List cookies
        -byte[] body
        -boolean isAttachment
        -String filename
        +setStatus(int, String)
        +setHeader(String, String)
        +setBody(byte[])
        +addCookie(Cookie)
        +send(OutputStream)
    }
    
    class Cookie {
        -String name
        -String value
        -int maxAge
        -String path
        -boolean httpOnly
        +getName()
        +getValue()
        +toHeaderString()
        +static parse(String)
    }
    
    class ConnectionManager {
        -Socket socket
        -HttpRequest request
        -HttpResponse response
        +run()
    }
    
    class RequestRouter {
        +findHandler(HttpRequest)
    }
    
    class Handler {
        <<interface>>
        +processRequest(HttpRequest, HttpResponse)
    }
    
    class AdminHandler {
        +processRequest(HttpRequest, HttpResponse)
    }
    
    class FileHandler {
        +processRequest(HttpRequest, HttpResponse)
    }
    
    class ResponseBuilder {
        +getOkResponse()
        +getNotFoundResponse()
        +getUnauthorizedResponse()
    }
    
    App --> SimpleWebServer : creates
    SimpleWebServer --> ConnectionManager : creates for each connection
    ConnectionManager --> HttpRequest : parses from socket
    ConnectionManager --> RequestRouter : passes request to
    RequestRouter --> Handler : finds appropriate
    Handler <|-- AdminHandler : implements
    Handler <|-- FileHandler : implements
    Handler --> ResponseBuilder : uses to create responses
    Handler --> HttpResponse : populates
    ConnectionManager --> HttpResponse : sends to client
    HttpResponse --> Cookie : may contain
    HttpRequest --> Cookie : may parse from
```

> Note: This class diagram shows the relationships between the main components of the HTTP Server implementation. The diagram is optimized for readability with a size of 1600x1200 pixels.
