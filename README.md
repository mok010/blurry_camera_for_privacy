# 블러링 카메라

### 날개팀 (2024.05.03 ~ )

### **2024년 졸업 프로젝트**


#### 사진에 노출되는 개인정보를 안전하게 보호하기 위해 홍채 및 지문을 자동 블러링 처리 해주는 카메라 어플리케이션.


A camera application that automatically blurs iris and fingerprints to safely protect personal information exposed in photos.

# Role

|                            이름                             |              역할              |                           책임                            |
| :---------------------------------------------------------: | :----------------------------: | :-------------------------------------------------------: |
|   [mok010](https://github.com/mok010)     |       Team Leader 👑, Android        |                전체적인 프로젝트 관리 담당                |
|   [minus_k](https://github.com/minus43)   |        Android         |               Android 기능 구현 및 관리              |
|   [saesongtree](https://github.com/saesongtree)   |        Android         |               Android 기능 구현 및 관리               |
|   [CHOYONGGEUN](https://github.com/CHOYONGGEUN)   |        Android         |            Android 기능 구현 및 관리                  |
|   [hanjjaeni](https://github.com/hanjjaeni)   |        Android         |              Android 기능 구현 및 관리                |

-------------------

### **1. 메인 기능**

#### - **홍채 블러링** : 
홍채를 이미지에서 분별하여 흐릿하게 변환. (ON / OFF)

#### - **지문 블러링** : 
지문을 이미지에서 분별하여 흐릿하게 변환. (ON / OFF)

#### - **사진 촬영** : 
카메라로 촬영 및 블러링 기능 자동 수행.

#### - **앨범** : 
앨범에서 불러온 이미지를 블러링 기능 자동 수행.

<br>

### **1. Main function**

#### - **Iris blurring**: 
Identify the iris in the image and convert it to blurry. (ON/OFF)

#### - **Fingerprint blurring**: 
Fingerprints are identified in the image and converted to blurry. (ON/OFF)

#### - **Photo taking**: 
Take photos with the camera and automatically perform blurring functions.

#### - **Album**: 
Automatically performs the blurring function on images loaded from the album.




-------------------

### **2.개발 도구**

#### - **Android Studio** : 전체적인 코드 구현
#### - **Kakao Oven** : 구체적인 UI 스케치
#### - **ML Kit**: 동공 및 손가락 탐지를 위한 머신러닝 기반 탐지 기능 적용

<br>

### **2.Development Tools**

#### - **Android Studio**: Overall code implementation
#### - **Kakao Oven**: Specific UI sketch
#### - **ML Kit**: Applied for machine learning-based detection of pupils and fingers


-------------------

## 📌 이 프로젝트에 대해 (about Project)

### ***1. 프로젝트 정보 (Project Information)***

#### ✔ 프로젝트 이름 : 
개인정보 블러링 카메라 

#### ✔ 프로젝트 설명 : 
카메라와 사진 수정 기능을 수행한다. 이 과정에서 홍채와 지문을 블러링하여 개인 생체정보를 보호 할 수 있다. 또한 홍채와 지문 블러링 기능에 대해 원하는 대로 on/off 할 수 있다. 

#### ✔ 프로젝트 목적 : 
생체인식 기술은 스마트폰, 컴퓨터, 출입 통제 시스템, 금융 거래 등에서 빠르게 확산되고 있으며, 사용자들에게 편리성과 보안성을 제공하는 중요한 기술로 자리 잡고 있다. 예를 들어, 애플의 Face ID와 삼성의 지문 인식 기술은 스마트폰에서 개인의 신원을 확인하고, 결제 인증 수단으로도 널리 사용된다. 코로나19 팬데믹 이후 비대면 서비스와 원격 업무의 증가로 인해, 생체인식 기술은 더욱 중요한 인증 수단으로 주목받고 있다. 예를 들어, 병원이나 금융기관에서는 환자와 고객을 원격으로 확인하고 민감한 거래를 처리할 때 지문, 홍채 또는 얼굴 인식을 사용하기도 한다​. 이러한 기술은 사용자가 비밀번호를 기억할 필요가 없고, 복잡한 인증 절차를 간소화함으로써 많은 이점을 제공한다. 그러나 그 이면에는 생체정보 유출에 대한 위험이 함께 증가하고 있다. 생체정보가 의도치 않게 유출되는 가장 큰 경로 중 하나는 SNS와 같은 온라인 플랫폼이다. 사람들은 일상적인 순간을 담은 사진을 자주 공유하지만, 이 사진 속에는 얼굴, 손가락, 눈 등 중요한 생체정보가 담겨 있을 수 있다. 하지만 이를 반대로 말한다면 본인이 포스팅하는 매체만 신경 써도 많은 범죄노출을 줄일 수 있다는 것이다. 우리는 이 애플리케이션을 통해 간단한 조작으로 힘들지 않게 생체정보를 블러링 할 수 있도록 하였다

<br>

#### ✔ Project Name: 
Personal Information Blurring Camera

#### ✔ Project Description: 
This project involves camera and photo editing functionalities, focusing on blurring irises and fingerprints to protect personal biometric information. Users can toggle the iris and fingerprint blurring features on or off according to their preference.

#### ✔ Project Objective: 
Biometric recognition technology is rapidly spreading across various fields, including smartphones, computers, access control systems, and financial transactions, providing both convenience and security to users. For example, Apple’s Face ID and Samsung’s fingerprint recognition are widely used for identity verification and payment authentication on smartphones. With the increase in remote services and work-from-home trends post-COVID-19 pandemic, biometric recognition technology has become even more critical as a means of authentication. Hospitals and financial institutions, for example, use fingerprint, iris, or facial recognition to verify patients and customers remotely and process sensitive transactions. These technologies offer numerous benefits by eliminating the need for users to remember passwords and simplifying complex authentication processes. However, there is an increasing risk of biometric information leakage. One of the main channels of unintentional leakage is online platforms, such as social media. People often share everyday moments in photos, which may contain important biometric information like faces, fingers, and eyes. By focusing on the platforms on which one posts, one can significantly reduce exposure to crimes. This application aims to allow users to easily blur biometric information with simple controls, helping to prevent potential risks.

<br>

#### ✔ 간단한 사용 방법 및 예제(Simple Usage Instructions and Examples):
![워크플로우](https://github.com/user-attachments/assets/08432bdd-bf29-4bb9-b285-f1bdc8b3d105)

-------
### ***2. 프로젝트 결과물 (Project Deliverables)***

#### ✔ 예시 영상 (Example Video)

#### ✔ 예시 이미지 (Example Image)

![ex blur](https://github.com/user-attachments/assets/f8320432-9032-4ba9-8200-741263ace22c)

-------

### ***3. 기능 구현 (Implementation of Blurring Functionality)***

#### ✔ ML Kit이 제공하는 랜드마크 (Landmarks Provided by ML Kit) :
![랜드마크](https://github.com/user-attachments/assets/588e5006-e893-4cf9-9217-1becdee35c2d)


#### ✔ 홍채 블러링 / 지문 블러링 on/off 버튼:
- 사용자가 동공 또는 지문 블러링 여부를 선택할 수 있는 맞춤형 버튼을 제공하여 원하는 부분만 블러링 처리할 수 있다. 이를 통해 사용자는 더 유연하게 보호 기능을 사용할 수 있다.
- 
#### ✔ 홍채 블러링 : 
- ML Kit의 랜드마크 탐지 기능을 활용하여 민감한 정보를 보호하기 위해 해당 영역을 블러 처리하는 기능을 구현하였다. ML Kit에서 제공하는 랜드마크 중 눈동자 탐지 랜드마크(2번, 5번)를 사용하여 해당위치에 인간의 평균 눈동자 크기(얼굴 크기의 3% 크기)의 영역을 구해 반투명한 이미지를 덧씌운다. 반투명 이미지의 경우 해당 영역의 해상도를 떼어 낮춘 후 다시 그 위치에 덧씌우는 원리이다.

#### ✔ 지문 블러링 : 
- ML Kit의 랜드마크 탐지 기능을 활용하여 민감한 정보를 보호하기 위해 해당 영역을 블러 처리하는 기능을 구현하였다. ML Kit의 경우 손목, 손바닥(검지, 소지 손가락 뿌리)과 엄지의 랜드마크만을 제공한다. 따라서 지문의 위치인 손가락 끝을 찾아내기 위해서는 몇 번의 처리과정이 더 필요했다. 검지뿌리(랜드마크 19번과 20번)의 좌표를 기반으로 손목(랜드마크 15번과 16번)까지의 거리, 소지뿌리(랜드마크 17번과 18번)의 좌표를 기반으로 손목(랜드마크 15번과 16번)까지의 거리를 계산하여 그 거리의 2배를 한 변으로 하는 사각형을 1차 탐지 영역으로 설정한다. 그 후 옷 소매 등에 가릴 수 있는 가능성이 가장 적다고 판단한 검지뿌리 랜드마크 좌표에서 픽셀을 가져온다. 이 픽셀의 색을 피부색으로 설정하고 이 색상의 근처 범위(색조 차이 10 이하, 채도 차이 20% 이하, 명도 차이 20% 이하)에 있는 픽셀만 1차 탐지 영역에서 뽑아온다. 이 영역을 리스트화 시키며, 이 영역이 최종적으로 ‘손영역’으로 표현 되는 2차 영역이 된다. 이 2차 영역에 블러링을 적용시킨다.

<br>

#### ✔ Iris Blurring / Fingerprint Blurring On/Off Buttons:

Customizable buttons that allow users to choose whether to blur their irises or fingerprints, enabling selective blurring of specific areas. This feature provides users with more flexible protection options.

#### ✔ Iris Blurring:

This feature utilizes ML Kit's landmark detection to blur sensitive areas and protect private information. By using the eye detection landmarks (points 2 and 5) provided by ML Kit, it identifies the locations of the irises. An area based on the average human iris size (approximately 3% of the face size) is calculated, and a translucent image is overlaid on top of this area. The translucent overlay is created by taking the original area’s resolution, reducing it, and then reapplying it to the same location to blur the details.

#### ✔ Fingerprint Blurring:
This feature also uses ML Kit's landmark detection to blur sensitive areas and protect private information. ML Kit provides landmarks for the wrist, the base of the hand (index and pinky finger bases), and the thumb. However, additional processing was needed to locate the fingerprint area at the fingertip. Based on the coordinates of the index base landmarks (points 19 and 20), the distance to the wrist landmarks (points 15 and 16) is calculated, and the same is done for the pinky base landmarks (points 17 and 18). A square with twice this distance as its side length is set as the primary detection area. From the index base landmark coordinates, which are the least likely to be obscured by sleeves, pixel data is collected. The color of these pixels is set as the skin tone, and only pixels within a defined range (hue difference under 10, saturation difference under 20%, brightness difference under 20%) are selected from the primary detection area. This list of pixels represents the final "hand area," which is the secondary detection area. Blurring is then applied to this secondary area.

  
