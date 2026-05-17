# DeepCode v1.2.0 - Hướng Dẫn Cài Đặt và Sử Dụng

## 🏠 Giới thiệu

**DeepCode** là hệ thống phân tích code Competitive Programming sử dụng AI để:
- Crawl code từ Codeforces và VJudge
- Phân tích thuật toán và cấu trúc dữ liệu được sử dụng
- Đánh giá mức độ sử dụng AI trong code
- Đánh giá năng lực CTDL/Thuật toán của lập trình viên

---

## 🏗️ Kiến trúc hệ thống

### Mô hình 3 lớp

```
┌─────────────────────────────────────────┐
│  UI Layer (JavaFX)                      │
│  ┌─────────────────────────────────┐    │
│  │  MainController                 │    │
│  │  ├── UserController             │    │
│  │  ├── AnalysisController         │    │
│  │  ├── ReportController           │    │
│  │  └── SettingsController         │    │
│  └─────────────────────────────────┘    │
├─────────────────────────────────────────┤
│  Business Logic Layer                   │
│  ┌─────────────────────────────────┐    │
│  │  SchedulerService               │    │
│  │  ├── CrawlerService             │    │
│  │  │   ├── CodeforcesAPI          │    │
│  │  │   └── VJudgeAPI              │    │
│  │  └── AIAnalyzer (Gemini)        │    │
│  └─────────────────────────────────┘    │
├─────────────────────────────────────────┤
│  Data Layer                             │
│  ┌─────────────────────────────────┐    │
│  │  DatabaseManager (SQLite)       │    │
│  │  ├── UserDAO                    │    │
│  │  ├── SubmissionDAO              │    │
│  │  └── AnalysisDAO                │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

### Thư viện chính

| Thư viện | Phiên bản | Mục đích |
|----------|-----------|----------|
| JavaFX | 21.0.2 | Giao diện người dùng |
| SQLite JDBC | 3.45.1.0 | Lưu trữ dữ liệu |
| Gson | 2.10.1 | Xử lý JSON (phân tích AI response) |
| Jsoup | 1.17.2 | Parse HTML (crawl source code) |

---



### Mô tả các package

| Package | Mô tả |
|---------|-------|
| `controller` | Xử lý sự kiện UI, điều hướng giữa các tab |
| `dao` | Data Access Objects - thao tác với SQLite database |
| `model` | Các class dữ liệu: User, Submission, AnalysisResult, UserEvaluation |
| `service` | Business logic: crawl code, phân tích AI, scheduler |
| `util` | Tiện ích: Config manager đọc/ghi file properties |

---

## 💻 Yêu cầu hệ thống

| Thành phần | Yêu cầu tối thiểu |
|------------|-------------------|
| Java | JDK 17 trở lên |
| RAM | 4 GB |
| Dung lượng đĩa | 200 MB |
| Mạng | Internet (để crawl code) |
| Gemini API Key | Cần thiết để phân tích AI |

### Kiểm tra phiên bản Java:
```powershell
java -version
```

---

## 📥 Hướng dẫn cài đặt

### Cách 1: Build từ source code

#### Bước 1: Cài đặt JDK 17+
- Windows: Tải từ https://adoptium.net/
- Kiểm tra: `java -version`

#### Bước 2: Cài đặt Maven

**Cách 1: Tải thủ công**
1. Truy cập https://maven.apache.org/download.cgi
2. Tải file `apache-maven-3.9.x-bin.zip`
3. Giải nén vào thư mục mong muốn (ví dụ: `C:\apache-maven-3.9.15-bin`)
4. Thêm đường dẫn `bin` vào PATH:
   - Mở **System Properties** → **Environment Variables**
   - Chọn **Path** trong **System variables** → **Edit**
   - Thêm đường dẫn: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin`
   - Nhấn **OK** và mở terminal mới

**Cách 2: Sử dụng Chocolatey**
```powershell
choco install maven -y
```

**Kiểm tra cài đặt:**
```powershell
mvn -version
```

> **Yêu cầu:** Đảm bảo biến môi trường `JAVA_HOME` đã được trỏ đến JDK 17+.

#### Bước 3: Clone/Download dự án
```powershell
# Nếu có git
git clone <repository-url>
cd DeepCode-1.2.0

# Hoặc giải nén file zip đã tải
```

#### Bước 4: Build dự án
```powershell
# Di chuyển vào thư mục dự án
cd DeepCode

# Build với Maven
mvn clean package

# Hoặc chạy trực tiếp
mvn javafx:run
```

#### Bước 5: Chạy ứng dụng
```powershell
# Sau khi build thành công, file JAR sẽ nằm ở
# target/deepcode-1.2.0.jar

java -jar target/deepcode-1.2.0.jar
```

### Cách 2: Sử dụng file đã build sẵn
```powershell
# Chạy trực tiếp file JAR
java -jar deepcode-1.2.0.jar
```

---

## 📖 Hướng dẫn sử dụng

### Giao diện chính

Giao diện gồm 4 phần chính:
1. **Quản lý Nick** - Thêm/sửa/xóa tài khoản
2. **Phân tích Code** - Phân tích bằng AI
3. **Báo cáo & Đánh giá** - Xem kết quả đánh giá
4. **Cài đặt** - Cấu hình hệ thống

### 1. Thêm Nick mới

1. Mở tab **"Quản lý Nick"**
2. Nhập username vào ô "Nhập username"
3. Chọn nền tảng (Codeforces hoặc VJudge)
4. Bấm **"Thêm Nick"**
5. Hệ thống sẽ tự động kiểm tra username trên nền tảng tương ứng

### 2. Crawl Code

#### Crawl thủ công:
1. Chọn nick trong bảng
2. Bấm **"Crawl Code ngay"** hoặc **"Crawl tất cả"**
3. Theo dõi tiến trình trong cửa sổ Log

#### Crawl tự động:
- Hệ thống tự động crawl theo lịch đã cấu hình (mặc định 24 giờ/lần)
- Có thể thay đổi trong tab **"Cài đặt"**

### 3. Phân tích Code bằng AI

1. Mở tab **"Phân tích Code"**
2. Chọn user cần phân tích
3. Bấm **"Phân tích AI cho User này"**
4. Chờ AI phân tích từng submission
5. Xem kết quả trong bảng bên phải

> **Lưu ý:** Cần có Gemini API Key để sử dụng chức năng phân tích AI.

### 4. Xem Báo cáo Đánh giá

1. Mở tab **"Báo cáo & Đánh giá"**
2. Chọn user trong bảng
3. Xem chi tiết đánh giá:
   - **Điểm CTDL**: Trình độ sử dụng cấu trúc dữ liệu
   - **Điểm Thuật toán**: Trình độ thuật toán
   - **Mức dùng AI**: Tỷ lệ code có dấu hiệu được tạo bởi AI

### 5. Cài đặt

Tab cài đặt cho phép:
- Nhập **Gemini API Key**
- Thay đổi **khoảng cách crawl** (giờ)
- Thay đổi **số lượng submissions** tối đa mỗi lần crawl
- Chọn **Gemini Model** sử dụng
- **Crawl tất cả ngay** - chạy crawl thủ công cho tất cả users

---

## ⚙ Cấu hình hệ thống

### File cấu hình: `deepcode.properties`

```properties
# API Key cho Gemini (bắt buộc)
gemini.api.key=YOUR_API_KEY_HERE

# Khoảng cách crawl (giờ)
crawl.interval.hours=24

# Số submissions tối đa mỗi lần crawl
crawl.max.submissions=50

# Đường dẫn database
db.path=deepcode.db

# Gemini Model
gemini.model=gemini-2.0-flash

# Gemini API URL
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/
```

### Lấy Gemini API Key

1. Truy cập: https://aistudio.google.com/app/apikey
2. Đăng nhập bằng tài khoản Google
3. Bấm **"Create API key"**
4. Copy API key và dán vào ô **"Google Gemini API Key"** trong tab Cài đặt

### Các model Gemini được hỗ trợ

| Model | Mô tả |
|-------|-------|
| gemini-2.0-flash | Nhanh, miễn phí (khuyến nghị) |
| gemini-1.5-flash | Cân bằng giữa tốc độ và chất lượng |
| gemini-1.5-pro | Chất lượng cao hơn |

---

## 🔧 Xử lý sự cố

### Lỗi thường gặp

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| "Gemini API key chưa được cấu hình" | Chưa nhập API Key | Vào Cài đặt > Nhập API Key |
| "Không tìm thấy user" | Username không tồn tại | Kiểm tra lại username |
| "Database initialization failed" | Lỗi SQLite | Xóa file deepcode.db và chạy lại |
| Crawl chậm | Rate limit API | Giảm số submissions hoặc tăng delay |
| "Connection timeout" | Mất kết nối mạng | Kiểm tra kết nối Internet |

### Xóa database và bắt đầu lại
```powershell
# Xóa file database
Remove-Item deepcode.db

# Chạy lại ứng dụng
java -jar target/deepcode-1.2.0.jar
```

### Kiểm tra log
Logs được hiển thị trực tiếp trên giao diện ứng dụng. Trong trường hợp crash, kiểm tra:
```powershell
# Kiểm tra console output khi chạy từ terminal
java -jar target/deepcode-1.2.0.jar
```

---

## 📊 Kết quả đánh giá

### Cách tính điểm

#### Điểm CTDL (Cấu trúc dữ liệu)
- Dựa trên variety (đa dạng) và frequency (tần suất sử dụng)
- CTDL nâng cao (Segment Tree, Trie, Graph) được cộng điểm cao hơn
- Thang điểm: 0-100

#### Điểm Thuật toán
- Thuật toán phức tạp (DP, Network Flow, FFT) được cộng điểm cao
- Brute Force có điểm thấp nhất
- Thang điểm: 0-100

#### Mức độ sử dụng AI
- Phân tích các dấu hiệu:
  - Variable naming quá mô tả
  - Code structure quá hoàn hảo
  - Comments quá chi tiết
  - Template-like patterns
- Thang điểm: 0-100%

### Mức đánh giá

| Mức | Điểm CTDL/Thuật toán | Mức dùng AI |
|-----|---------------------|-------------|
| ⭐ Xuất sắc | >= 80 | - |
| 🟢 Tốt | 60-79 | <= 25% |
| 🟡 Trung bình | 40-59 | 26-50% |
| 🟠 Yếu | 20-39 | 51-75% |
| 🔴 Rất yếu/Rất cao | < 20 | > 75% |

---

## 🤝 Đóng góp

1. Fork repository
2. Tạo feature branch: `git checkout -b feature/ten-tinh-nang`
3. Commit thay đổi: `git commit -m "Mô tả tính năng"`
4. Push lên branch: `git push origin feature/ten-tinh-nang`
5. Tạo Pull Request

### Quy tắc code
- Tuân thủ Java naming conventions
- Viết comment cho các method public
- Test trước khi submit PR

---

## 📝 Ghi chú

- Database SQLite tự động tạo khi chạy lần đầu
- File cấu hình `deepcode.properties` được tạo trong thư mục chạy ứng dụng
- Crawl định kỳ chạy ngầm khi ứng dụng đang mở
- Đóng ứng dụng sẽ tự động lưu trạng thái và đóng database

---

**Phiên bản:** 1.2.0  
**Cập nhật:** 2026
