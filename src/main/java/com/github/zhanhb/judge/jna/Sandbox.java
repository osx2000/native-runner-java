package com.github.zhanhb.judge.jna;

import static com.github.zhanhb.judge.jna.Kernel32.*;
import static com.github.zhanhb.judge.jna.Kernel32.JOBOBJECTINFOCLASS.*;
import com.github.zhanhb.judge.jna.Kernel32.JOBOBJECT_BASIC_LIMIT_INFORMATION;
import com.github.zhanhb.judge.jna.Kernel32.JOBOBJECT_BASIC_UI_RESTRICTIONS;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import java.io.Closeable;

public class Sandbox implements Closeable {

    private final WinNT.HANDLE hJob;

    public Sandbox() {
        hJob = Kernel32.INSTANCE.CreateJobObject(null, null);
        Kernel32Util.assertTrue(hJob != null && hJob.getPointer() != null);

        boolean success = false;
        try {
            JOBOBJECT_BASIC_LIMIT_INFORMATION jobli = new JOBOBJECT_BASIC_LIMIT_INFORMATION();
            jobli.ActiveProcessLimit = 1;
            // These are the only 1 restrictions I want placed on the job (process).
            jobli.LimitFlags = JOB_OBJECT_LIMIT_ACTIVE_PROCESS;
            Kernel32Util.assertTrue(Kernel32.INSTANCE.SetInformationJobObject(hJob, JobObjectBasicLimitInformation, jobli, jobli.size()));

            // Second, set some UI restrictions.
            JOBOBJECT_BASIC_UI_RESTRICTIONS jobuir = new JOBOBJECT_BASIC_UI_RESTRICTIONS();
            jobuir.UIRestrictionsClass
                    = // The process can't access USER objects (such as other windows)
                    // in the system.
                    JOB_OBJECT_UILIMIT_HANDLES
                    | JOB_OBJECT_UILIMIT_READCLIPBOARD
                    | JOB_OBJECT_UILIMIT_WRITECLIPBOARD
                    | JOB_OBJECT_UILIMIT_SYSTEMPARAMETERS
                    | JOB_OBJECT_UILIMIT_DISPLAYSETTINGS
                    | JOB_OBJECT_UILIMIT_GLOBALATOMS
                    | // Prevents processes associated with the job from creating desktops
                    // and switching desktops using the CreateDesktop and SwitchDesktop functions.
                    JOB_OBJECT_UILIMIT_DESKTOP
                    | // The process can't log off the system.
                    JOB_OBJECT_UILIMIT_EXITWINDOWS;
            Kernel32Util.assertTrue(Kernel32.INSTANCE.SetInformationJobObject(hJob, JobObjectBasicUIRestrictions, jobuir,
                    jobuir.size()));
            success = true;
        } finally {
            if (!success) {
                SafeHandle.close(hJob);
            }
        }
    }

    public void beforeProcessStart(HANDLE hProcess) {
        Kernel32Util.assertTrue(Kernel32.INSTANCE.AssignProcessToJobObject(hJob, hProcess));
    }

    @Override
    public void close() {
        SafeHandle.close(hJob);
    }

}
