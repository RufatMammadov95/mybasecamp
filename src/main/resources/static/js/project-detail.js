document.addEventListener("DOMContentLoaded", function() {
    var activePanelFromBack = window.serverActivePanel;
    var savedPanel = localStorage.getItem("activeProjectPanel");

    if (performance.navigation.type === 1) {
        document.querySelectorAll('#members-error-alert, #members-success-alert, #topics-error-alert, #topics-success-alert')
            .forEach(el => el.remove());
    }

    if (activePanelFromBack && activePanelFromBack !== "null" && activePanelFromBack !== "") {
        if (!activePanelFromBack.startsWith("panel-")) {
            activePanelFromBack = "panel-" + activePanelFromBack;
        }
        showPanel(activePanelFromBack);
    } else if (savedPanel) {
        showPanel(savedPanel);
    } else {
        showPanel("panel-overview");
    }
});

function showPanel(panelId) {
    if (panelId && !panelId.startsWith("panel-")) {
        panelId = "panel-" + panelId;
    }

    if (panelId !== "panel-members") {
        var mErr = document.getElementById("members-error-alert"); if (mErr) mErr.remove();
        var mSucc = document.getElementById("members-success-alert"); if (mSucc) mSucc.remove();
    }

    if (panelId !== "panel-topics") {
        var tErr = document.getElementById("topics-error-alert"); if (tErr) tErr.remove();
        var tSucc = document.getElementById("topics-success-alert"); if (tSucc) tSucc.remove();
    }

    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    var targetPanel = document.getElementById(panelId);
    if (targetPanel) {
        targetPanel.classList.add('active');
        localStorage.setItem("activeProjectPanel", panelId);
    }
}

function toggleEditDiscussion() {
    document.getElementById("disc-title-text").style.display = "none";
    document.getElementById("disc-header-actions").style.display = "none";
    document.getElementById("disc-title-form").style.display = "flex";
}

function cancelEditDiscussion() {
    document.getElementById("disc-title-text").style.display = "inline";
    document.getElementById("disc-header-actions").style.display = "flex";
    document.getElementById("disc-title-form").style.display = "none";
}

function toggleEditMessage(msgId) {
    document.getElementById("msg-text-" + msgId).style.display = "none";
    document.getElementById("msg-form-" + msgId).style.display = "flex";
}

function cancelEditMessage(msgId) {
    document.getElementById("msg-text-" + msgId).style.display = "block";
    document.getElementById("msg-form-" + msgId).style.display = "none";
}