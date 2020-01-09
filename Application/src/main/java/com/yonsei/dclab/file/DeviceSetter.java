package com.yonsei.dclab.file;

public class DeviceSetter {
    String PatientNum;
    String LeftNum;
    String RightNum;
    int AlarmThreshold = 8200;

    public void setter (String patient_id){
        switch (patient_id){
            case "11":
                PatientNum = "01";
                LeftNum = "DA:C7:9A:F5:27:85";
                RightNum = "EF:75:65:27:A4:CD";
                AlarmThreshold = 7986;
                break;
            case "B":
                PatientNum = "02";
                LeftNum = null;
                RightNum = null;
                break;
            case "C":
                PatientNum = "03";
                LeftNum = null;
                RightNum = null;
                break;
            case "14":
                PatientNum = "04";
                LeftNum = "CA:2E:9A:CE:A8:03";
                RightNum = "CC:70:FA:98:D4:54";
                AlarmThreshold = 8208;
                break;
            case "E":
                PatientNum = "05";
                LeftNum = null;
                RightNum = null;
                break;
            case "06":
                PatientNum = "06";
                LeftNum = "FE:55:A5:71:F0:0B";
                RightNum = "D2:40:D3:0A:06:8B";
                AlarmThreshold = 8176;
                break;
            case "07":
                PatientNum = "07";
                LeftNum = "FC:13:52:E1:B2:25";
                RightNum = "F7:EB:5E:DA:D7:AC";
                AlarmThreshold = 8187;
                break;
            case "08":
                PatientNum = "08";
                LeftNum = "D8:43:C1:69:13:A0";
                RightNum = "F4:71:9E:A0:3D:25";
                AlarmThreshold = 8097;
                break;
            case "09": // I
                PatientNum = "09";
                LeftNum = "FF:1A:D5:8C:27:7C";
                RightNum = "D8:C8:EB:B9:03:DE";
                AlarmThreshold = 8272;
                break;
            case "0A"://J
                PatientNum = "10";
//                LeftNum = null;
//                RightNum = null;
//                LeftNum = "DC:46:3B:61:67:15";//M
                RightNum = "F7:65:AE:7D:81:7B";//M
//                LeftNum = "E7:E0:46:6C:C1:0F";//N
                LeftNum = "ED:FA:59:4C:73:0F";//N
                AlarmThreshold = 8160;
                break;
            case "1E":
                PatientNum = "11";
//                LeftNum = "E7:A8:E1:3C:33:66";
//                RightNum = "E1:BD:92:6C:32:E0";
                AlarmThreshold = 8180;
                break;
            case "L":
                PatientNum = "12";
                LeftNum = null;
                RightNum = null;
                break;
            case "0D":
                PatientNum = "13";
//                LeftNum = "DC:46:3B:61:67:15";//M
//                RightNum = "F7:65:AE:7D:81:7B";//M
                LeftNum = "E7:A8:E1:3C:33:66";
                RightNum = "E1:BD:92:6C:32:E0";
                AlarmThreshold = 8306;
                break;
            case "0E":
                PatientNum = "14";
//                LeftNum = "E7:E0:46:6C:C1:0F";//N
//                RightNum = "ED:FA:59:4C:73:0F";//N
                LeftNum = "CA:2E:9A:CE:A8:03";
                RightNum = "CC:70:FA:98:D4:54";
                AlarmThreshold = 8160;
                break;
            case "0F":
                PatientNum = "15";
                LeftNum = "C4:01:92:F3:0D:4A";
                RightNum = "F3:01:E7:BB:4D:70";
                AlarmThreshold = 8322;
                break;
        }

    }

    public String getPatientNum() {
        return PatientNum;
    }
    public String getLeftNum(){
        return LeftNum;
    }
    public String getRightNum(){
        return RightNum;
    }
    public int getAlarmThreshold(){
        return AlarmThreshold;
    }


}
