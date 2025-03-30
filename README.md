# Smart City with ANPR Parking System

本项目是一个智能城市解决方案，集成了基于自动车牌识别（ANPR）的停车系统。该解决方案包含两个主要界面：

1. **用户端（Android App）**  
2. **管理端（Web 管理界面，含 ANPR 功能）**

所有注释与界面均面向英文用户，因此本文件中涉及的功能说明以英文为准，方便英文用户使用和维护。

---

## 目录
- [概述](#概述)
- [功能简介](#功能简介)
  - [1. 用户端（Android App）](#1-用户端android-app)
  - [2. 管理端（Web 管理界面）](#2-管理端web-管理界面)
  - [3. ANPR 程序](#3-anpr-程序)
- [使用技术](#使用技术)
- [Firebase 数据结构（推荐）](#firebase-数据结构推荐)
- [安装与使用](#安装与使用)
- [使用说明](#使用说明)
- [License](#license)

---

## 概述
这是一个模拟智能停车场景的解决方案，利用了 ANPR（自动车牌识别）技术来简化停车管理流程。它包括：

- 面向用户的 **Android 应用**（注册、支付停车费用、地图定位、评论等）
- 面向管理员的 **Web 管理界面**（监控车辆、读取车牌信息以及管理停车费用）
- 基于 **OCR** 技术的 **ANPR 程序**（识别并处理车牌号码及国家标识）

---

## 功能简介

### 1. 用户端（Android App）
**用户端** 是一个安卓应用，提供注册、登录、查看附近停车场信息、支付停车费用、查看/提交停车评价等功能。

1.1 **用户注册与登录**  
   - 用户通过 **邮箱/密码**、**ID Name** 及 **车牌号** 完成注册（默认车牌格式为英国车牌）。  
   - 使用 [Firebase Authentication](https://firebase.google.com/docs/auth) 存储用户账号信息。  
   - 提供密码找回功能（忘记密码），通过 Firebase 完成。  

1.2 **地图与停车场位置**  
   - 可在地图上查看英国地区约 20 个停车场分布。这些停车场必须是真实的停车场信息  
   - 默认地图位置定位在 **伦敦（London, UK）**。  
   - 支持通过 **邮编搜索** 在地图上展示搜索结果。  
   - 停车场使用带有 "P" 标记的地图 Marker 显示。  

1.3 **支付**  
   - 应用中展示用户的 **进场时间**、**出场时间**（均从 Firebase 获取）。  
   - 提示计算出的 **停车费用**（同样从 Firebase 获取）。  
   - 集成演示用的 **Stripe API** 支付功能（仅作演示，不进行真实支付）。  

1.4 **停车详情与评价**  
   - 点击停车场 Marker 后弹出二级菜单/对话框，其中包含：  
     - 停车场名称、地址、评级、价格、总车位数、开放时间等信息。  
     - 导航按钮（可跳转到 Google Maps 等第三方导航应用）。  
     - 用户评论区域：查看已有评论或添加新的反馈。  
     -右上角要有关闭菜单的按键

1.5 **个人中心（User Center）**  
   - 点击地图界面中的 **用户图标** 可进入个人中心，包含：  
     - **用户信息**（用户名、邮箱、车牌号）并可进行修改。  
       - 邮箱修改需要额外进行 Firebase 验证。  
       - 其他信息修改同样存储于 Firebase。  
     - **当前订单**（车牌号、开始时间等）以及 "Pay Now" 按钮来进行支付流程。  
     -加入log out
     -允许用户加入多个车牌信息

1.6 **Payment History**
   - This section allows users to view the complete history of parking activities associated with their registered license plates.
   - Features include:
     - Chronological list of all parking sessions (sorted by entry time, newest first)
     - Display of essential information for each record: license plate, entry time, exit time, fee amount, and payment status
     - Clear visual distinction between paid and unpaid records
     - "Pay Now" button for unpaid records that redirects to the payment processing screen
     - Automatic refresh of payment status when returning to the screen
   - The payment history is retrieved from Firebase Firestore using composite indexing for efficient queries
   - Implemented with a RecyclerView for smooth scrolling through potentially large lists of records

### 2. 管理端（Web 管理界面）
**管理端** 提供管理员登录、车牌信息处理与数据库数据查看的功能。

2.1. **管理员登录与找回**  
   - 管理员以 **邮箱** + **密码** 进行登录。  
   - 提供邮箱找回密码功能。  

2.2 **主界面**  
   - 显示 ANPR 流程与停车费用的概览。  
   - 可前往车牌读取页面或数据库查看页面。  

2.3 **车牌读取 / 上传界面**  
   - 管理员可从本地上传图像，或从预定义文件夹中获取图像进行车牌识别。  
   - 界面中展示图像转换为字符（OCR）过程。  
   - 主要元素：**上传照片**、**开始读取**、**处理状态**、**结果显示**。  
   - 读取结果展示识别到的车牌信息与国家标识(Country Identifier)，可返回主界面或到信息汇总页面。 
   — 不是用户注册时上传的车牌信息不录入数据库中，要提示：没有注册过不允许入场

2.4 **数据库查看**  
   - 显示注册过用户的车牌信息
   - 从 Firebase 获取并显示以下信息：  
     - **用户 ID**  
     - **车牌号**  
     - **国家标识** (Country Identifier，如"GB"代表英国)
     - **进场时间**  
     - **出场时间**  
     - **停车费用**  
     -**是否已经支付**
     

### 3. ANPR 程序
基于 Python 的 OCR 程序，用于自动识别车牌：
1. 从上传文件或本地目录 **识别车牌**  
2. 管理端可点击 "Start Reading" 读取指定文件夹或用户上传的照片  
3. 系统抽取**车牌号码**和**国家标识**(如"GB"代表英国)，并将结果存到 Firebase  
4. 支持识别蓝色区域中的"GB"标识和EU旗帜图标，以确定车牌来源国家



##5.HOME
5.1.**欢迎/问候语 + 用户概览**

以「欢迎回来，{用户名}」的形式，给用户一个友好直观的欢迎界面。

5.2.可在 Home 显示**离你最近的停车场**点击跳转到这个最近停车场的详情页中

5.3.**消息中心/通知**

在 Home 放置一个消息提示或通知中心入口，展示新的系统通知、支付提醒、活动通知等。
notifications里面包括未付款订单提醒，停车入场出场提醒,支付成功提醒

如果有新的消息，显示红点或数字提示，提醒用户查看。
用户可以点击查看通知内容
5.4。**新手指引/教学入口**

如果针对新用户，需要一些App 功能说明，可以在 Home 做一个显眼的「新手教程」入口。

对老用户则可选择折叠或隐藏。

5.5.**模块化排版**

整个 Home 建议采用「卡片式布局」或「分区式布局」，让不同功能板块层次分明，易于理解和点击。


---

## 使用技术
- **主要编程语言**：Python、Kotlin、HTML/JavaScript (Web 管理端)
- **框架与 API**：
  - **Firebase**（认证、实时数据库或 Firestore）
  - **Google Maps API**（地图与定位功能）
  - **Stripe API**（用于演示支付）
  - **OCR/计算机视觉**（Python 用于 ANPR）
- **安卓（Android）**（用户移动端应用）
- **Web**（管理界面）

---

## Firebase 数据结构（推荐）

以下为建议的 Firebase 数据结构，可根据需求进行调整：

```
Firebase Root
├── Users
│   └── {UserID}
│       ├── email: string
│       ├── password: string (hashed)
│       ├── name: string
│       ├── licensePlate: string
│       └── ...
├── ParkingSpots
│   └── {SpotID}
│       ├── name: string
│       ├── address: string
│       ├── rating: float
│       ├── totalSlots: number
│       ├── openHours: string
│       └── ...
├── Orders
│   └── {OrderID}
│       ├── userID: string
│       ├── licensePlate: string
│       ├── entryTime: timestamp
│       ├── exitTime: timestamp
│       ├── fee: float
│       └── ...
├── PlateRecognition
│   └── {RecognitionID}
│       ├── plate_number: string
│       ├── country_identifier: string
│       ├── confidence: float
│       ├── timestamp: timestamp
│       ├── exit_after_seconds: number
│       ├── fee: float
│       └── ...
├── Reviews
│   └── {SpotID}
│       └── {ReviewID}
│           ├── userID: string
│           ├── rating: float
│           ├── comment: string
│           └── ...
└── ...
```

Google map API Key:AIzaSyBANrmjCX9cNZcJC_aUO7UKZnZjMY7KnY0
Strip 公钥 pk_test_51R5tjcB0X5aGtngh2WehxbpvGjC8lbscoAtO8ThwAaYXUArhfW52Qs1Qhbljuj6EwIO6q96IIqXtZ7x4hKMSO1pA00n8Y147FM
Strip 密钥 sk_test_51R5tjcB0X5aGtnghsYGESgJTXn7xuo2VUbFvxvv9n8opIA3WIyAvocORqTFnCTq1AXa3cpTFAifNmAyhpJdBp2Hq003lcUmf3y

firebase web SDK npm
```javascript
// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyB7KmELhy13G7ThjqR1LyCL4aBmPEljVIg",
  authDomain: "parking-3e319.firebaseapp.com",
  projectId: "parking-3e319",
  storageBucket: "parking-3e319.firebasestorage.app",
  messagingSenderId: "1019635069018",
  appId: "1:1019635069018:web:73eca60da52adb8665e863",
  measurementId: "G-8ZK7N71MJ5"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
```



## Latest Achievements

We have successfully implemented several key features and improvements to enhance the Smart City ANPR system:

1. **UK License Plate Recognition** - The system accurately recognizes standard UK format license plates (e.g., "AA70 PYY")
2. **Country Identifier Recognition** - The system can recognize "GB" markings and EU flag icons on license plates
3. **Database Integration** - Recognition results are automatically stored in the Firebase database, including license plate numbers, country identifiers, timestamps, and calculated fees
4. **Complete Admin Interface** - The web interface allows real-time viewing of all recognized license plates and their country identifier information
5. **Payment History Feature** - Added functionality to view all parking records associated with user license plates, with clear distinction between paid and unpaid records
6. **Multiple License Plate Support** - Users can now register and manage multiple license plates from the User Center
7. **Improved Order Management** - The system now displays both entry and exit times in the Current Order section
8. **Enhanced Error Handling** - Implemented robust error handling for Firebase queries to improve application stability:
   - Replaced Tasks.whenAllSuccess with sequential processing of queries
   - Added proper error recovery for failed license plate queries
   - Implemented graceful fallback mechanisms for partial query failures
9. **User Experience Improvements** - Removed duplicate texts in dialogs and improved UI consistency
10. **Documentation Enhancement** - Updated documentation to be fully English-compatible for international users
11. **Development Workflow Optimization** - Fixed compilation errors and warnings to ensure smooth development process

The random exit time generation (1-20 seconds) and fee calculation (£5 per second) functions have also been completed, providing simulated real-world scenario data for testing.