# Hướng dẫn thiết lập dự án Java (backend)

Dự án này là phần backend Spring Boot cho ứng dụng bán giày sneaker. Tài liệu dưới đây hướng dẫn cách chuẩn bị môi trường, cấu hình kết nối, và chạy ứng dụng với JDK 25.

## Yêu cầu môi trường
- **JDK 25** (JAVA_HOME trỏ tới thư mục JDK 25).
- **Maven 3.9+** (đã kèm `./mvnw` trong repo, bạn có thể dùng wrapper mà không cần cài riêng Maven).
- **SQL Server** (mặc định kết nối tới `localhost:1433`).
- **Trình soạn thảo** hỗ trợ Lombok (IntelliJ IDEA/VS Code với plugin Lombok) để tránh lỗi cảnh báo biên dịch.

## Thiết lập JDK 25
1. Cài đặt JDK 25 (Temurin hoặc Oracle).
2. Xuất biến môi trường (ví dụ trên Linux/macOS):
   ```bash
   export JAVA_HOME="/path/to/jdk-25"
   export PATH="$JAVA_HOME/bin:$PATH"
   ```
3. Kiểm tra phiên bản:
   ```bash
   java -version
   ./mvnw -version
   ```

## Cấu hình ứng dụng
- File cấu hình chính: `src/main/resources/application.properties`.
- Thông số mặc định:
  - JDBC: `jdbc:sqlserver://localhost:1433;databaseName=DuAnTotNghiep_2;encrypt=true;trustServerCertificate=true;`
  - User: `sa`
  - Password: `123456`
- Điều chỉnh cho môi trường của bạn bằng cách:
  - Chỉnh sửa trực tiếp trong `application.properties`, **hoặc**
  - Ghi đè qua biến môi trường/`-D` khi chạy Maven, ví dụ:
    ```bash
    SPRING_DATASOURCE_URL="jdbc:sqlserver://dbhost:1433;databaseName=DuAnTotNghiep_2;encrypt=true;trustServerCertificate=true;" \
    SPRING_DATASOURCE_USERNAME=your_user \
    SPRING_DATASOURCE_PASSWORD=your_password \
    ./mvnw spring-boot:run
    ```
- Cấu hình SMTP (Gmail) cũng nằm trong `application.properties`; thay bằng thông tin email của bạn hoặc cấu hình qua biến môi trường tương tự.

## Cài đặt & chạy ứng dụng
1. Cài dependency và build kiểm tra nhanh:
   ```bash
   ./mvnw clean verify
   ```
2. Chạy ứng dụng (profile mặc định):
   ```bash
   ./mvnw spring-boot:run
   ```
3. Đóng gói jar runnable:
   ```bash
   ./mvnw clean package -DskipTests
   java -jar target/DuAnTotNghiep-0.0.1-SNAPSHOT.jar
   ```

## Ghi chú tương thích JDK 25
- `pom.xml` đã thiết lập `java.version` và `maven.compiler.release` về **25** cùng plugin `maven-compiler-plugin` 3.13.0 để biên dịch với JDK 25.
- Nếu IDE dùng Maven tích hợp, hãy kích hoạt Maven wrapper hoặc đặt JDK dự án về 25 để khớp cấu hình build.
