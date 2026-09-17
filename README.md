# PrePark - אפליקציה חכמה להזמנת חניות

## תיאור הפרויקט
PrePark היא אפליקציית אנדרואיד מתקדמת המאפשרת למשתמשים לאתר חניונים, לחשב מרחקים ליעד, ולשריין חניה עתידית או מיידית. האפליקציה מספקת חוויית משתמש חלקה (UI/UX) תוך שימוש בשירותי המיקום והמפות של Google, ושומרת את נתוני המשתמש והיסטוריית הפעולות שלו.

## פיצ'רים מרכזיים
* **חיפוש חכם והשלמה אוטומטית:** שילוב של Google Places API המאפשר הקלדת כתובות מהירה עם השלמה אוטומטית מדויקת (מוגבלת לישראל) ומניעת שגיאות קלט.

* **מפה אינטראקטיבית:** שילוב Google Maps SDK המציג את היעד הנבחר במפה ואת החניון הקרוב ביותר אליו.

* **חישוב מרחקים:** אלגוריתם המחשב אוטומטית את המרחק במטרים בין הכתובת שהוזנה לבין החניון הקרוב ביותר של הרשת.

* **מערכת הזמנות חכמה:** שימוש ב-BottomSheetDialog לבחירת זמן השהייה, כולל ולידציות תאריכים למניעת הזמנת חניה לשעות עבר או לקלטים בלתי חוקיים.

* **אזור אישי והיסטוריית חניות:** ניהול נתוני משתמש, אמצעי תשלום, ושמירת היסטוריית הזמנות.

## טכנולוגיות וספריות
* **שפת פיתוח:** Kotlin

* **עיצוב ממשק:** XML (דגש על ConstraintLayout להתאמה למסכים שונים)

* **ניווט:** Navigation Component למעבר חלק ובטוח בין מסכים (Fragments).

* **רכיבי שרת (Firebase Backend):**
  * **התחברות לאפליקציה:** ניהול והתחברות משתמשים באמצעות שירותי האימות של פיירבייס.
  * **ניהול נתונים בזמן אמת:** שימוש ב-Firebase Realtime Database לשיתוף דיווחי תפוסה ועדכוני חניונים בזמן אמת בין הנהגים.

* **מפות ומיקום:**
  * `com.google.android.gms:play-services-maps`
  * `com.google.android.libraries.places:places`

* **ניהול זיכרון ומחזורי חיים:** טיפול מקיף ב-LifeCycle של קומפוננטת המפה (MapView) למניעת קריסות וזליגות זיכרון במעבר בין מסכים או בהורדת האפליקציה לרקע.
<img width="175" height="386" alt="צילום מסך 2026-09-17 135624" src="https://github.com/user-attachments/assets/1f72c0ac-e9fe-4b00-8cb2-276f714e9cfe" />
<img width="166" height="374" alt="צילום מסך 2026-09-17 135712" src="https://github.com/user-attachments/assets/2582e3ea-dd3e-40fb-9506-c58052f6edda" />
<img width="170" height="374" alt="צילום מסך 2026-09-17 135725" src="https://github.com/user-attachments/assets/fa065baf-f04b-4af2-8367-01b866fb78c5" />
<img width="172" height="378" alt="צילום מסך 2026-09-17 135819" src="https://github.com/user-attachments/assets/3837361c-5781-4723-a8a4-99e927ef192f" />
<img width="167" height="374" alt="צילום מסך 2026-09-17 143658" src="https://github.com/user-attachments/assets/9f819521-3ce3-4e0f-ab4e-1f6b34628f4a" />
<img width="161" height="364" alt="צילום מסך 2026-09-17 143811" src="https://github.com/user-attachments/assets/accec1bb-5b06-4117-92b6-ce743dff1ba0" />
<img width="164" height="359" alt="צילום מסך 2026-09-17 143804" src="https://github.com/user-attachments/assets/236ec893-ac5d-4847-bae0-6671e833835a" />
<img width="175" height="378" alt="צילום מסך 2026-09-17 143749" src="https://github.com/user-attachments/assets/9c413898-7a01-454f-af66-52449ff4759b" />
<img width="172" height="369" alt="צילום מסך 2026-09-17 144047" src="https://github.com/user-attachments/assets/1c5f3e72-bb20-40ef-915c-7548ea40e51b" />
<img width="170" height="380" alt="צילום מסך 2026-09-17 144052" src="https://github.com/user-attachments/assets/7203d849-712a-4f87-b085-159b9750909f" />



https://youtu.be/GQUt9aZwb_w

https://youtu.be/xeLFbNVdU_s

https://youtube.com/shorts/B5fS9h3nHfo









