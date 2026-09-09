package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kizitonwose.calendarview.CalendarView;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.model.DayOwner;
import com.kizitonwose.calendarview.ui.DayBinder;
import com.kizitonwose.calendarview.ui.ViewContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;

public class AttendanceActivity extends AppCompatActivity {

    private CalendarView materialCalendar;
    private ImageView treeImage;
    private TextView treeStatus;
    private TextView attendanceCountText;
    private Button btnCheckAttendance;

    private final Set<LocalDate> attendanceDates = new HashSet<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private int ecoPoints = 0;
    private int attendanceCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        materialCalendar = findViewById(R.id.materialCalendar);
        treeImage = findViewById(R.id.treeImage);
        treeStatus = findViewById(R.id.treeStatus);
        attendanceCountText = findViewById(R.id.attendanceCountText);
        btnCheckAttendance = findViewById(R.id.btnCheckAttendance);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupCalendar();
        loadUserAttendanceInfo();
        loadAttendanceFromFirestore();

        btnCheckAttendance.setOnClickListener(v -> handleAttendance(LocalDate.now()));
    }

    private void setupCalendar() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(2);
        YearMonth endMonth = currentMonth.plusMonths(2);

        materialCalendar.setup(startMonth, endMonth, DayOfWeek.MONDAY);
        materialCalendar.scrollToMonth(currentMonth);

        materialCalendar.setDayBinder(new DayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                LocalDate date = day.getDate();
                boolean isCurrentMonth = day.getOwner() == DayOwner.THIS_MONTH;
                boolean isAttended = attendanceDates.contains(date);

                container.dayText.setText(String.valueOf(date.getDayOfMonth()));
                container.dayText.setAlpha(isCurrentMonth ? 1f : 0.35f);
                container.dayText.setBackgroundResource(isAttended ? R.drawable.bg_attendance_day_checked : android.R.color.transparent);
                container.dayText.setTextColor(getResources().getColor(isAttended ? android.R.color.white : R.color.text_primary));
                container.checkIcon.setVisibility(isAttended ? View.VISIBLE : View.GONE);
                container.itemView.setAlpha(isCurrentMonth ? 1f : 0.45f);
                container.itemView.setOnClickListener(v -> handleAttendance(date));
            }
        });
    }

    private void loadUserAttendanceInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long pointValue = documentSnapshot.getLong("ecoPoints");
                        Long attendanceValue = documentSnapshot.getLong("attendanceCount");

                        ecoPoints = pointValue != null ? pointValue.intValue() : 0;
                        attendanceCount = attendanceValue != null ? attendanceValue.intValue() : 0;

                        updateTreeGrowth();
                        updateStatusText();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "사용자 정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void loadAttendanceFromFirestore() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        attendanceDates.clear();

        db.collection("users")
                .document(currentUser.getUid())
                .collection("attendance")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String dateString = doc.getString("date");
                        if (dateString != null) {
                            attendanceDates.add(LocalDate.parse(dateString));
                        }
                    }

                    materialCalendar.notifyCalendarChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "출석 기록 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void handleAttendance(LocalDate date) {
        LocalDate today = LocalDate.now();

        if (!date.equals(today)) {
            Toast.makeText(this, "오늘 날짜만 출석할 수 있어요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (attendanceDates.contains(date)) {
            Toast.makeText(this, "이미 오늘 출석했어요.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        String dateKey = date.toString();
        AttendanceRecord record = new AttendanceRecord(dateKey, 1);

        db.collection("users")
                .document(uid)
                .collection("attendance")
                .document(dateKey)
                .set(record)
                .addOnSuccessListener(unused -> {
                    int newEcoPoints = ecoPoints + 1;
                    int newAttendanceCount = attendanceCount + 1;
                    int newGrowthStage = GrowthPolicy.getGrowthStage(newEcoPoints);

                    db.collection("users")
                            .document(uid)
                            .update(
                                    "ecoPoints", newEcoPoints,
                                    "attendanceCount", newAttendanceCount,
                                    "growthStage", newGrowthStage
                            )
                            .addOnSuccessListener(unused2 -> {
                                ecoPoints = newEcoPoints;
                                attendanceCount = newAttendanceCount;

                                attendanceDates.add(date);
                                materialCalendar.notifyDateChanged(date);

                                updateTreeGrowth();
                                updateStatusText();

                                Toast.makeText(this, "출석 완료! +1P", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "포인트 업데이트 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "출석 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void updateTreeGrowth() {
        int growthStage = GrowthPolicy.getGrowthStage(ecoPoints);
        if (growthStage == 1) {
            treeImage.setImageResource(R.drawable.tree_stage1);
        } else if (growthStage == 2) {
            treeImage.setImageResource(R.drawable.tree_stage2);
        } else {
            treeImage.setImageResource(R.drawable.tree_stage3);
        }
        treeStatus.setText(GrowthPolicy.getGrowthStatusText(ecoPoints));
    }

    private void updateStatusText() {
        attendanceCountText.setText("누적 출석: " + attendanceCount + "일 / 포인트: " + ecoPoints + "P");
    }

    public static class DayViewContainer extends ViewContainer {
        TextView dayText;
        ImageView checkIcon;
        View itemView;

        public DayViewContainer(View view) {
            super(view);
            itemView = view;
            dayText = view.findViewById(R.id.dayText);
            checkIcon = view.findViewById(R.id.checkIcon);
        }
    }

    public static class AttendanceRecord {
        public String date;
        public int pointsEarned;

        public AttendanceRecord() {
        }

        public AttendanceRecord(String date, int pointsEarned) {
            this.date = date;
            this.pointsEarned = pointsEarned;
        }
    }
}
