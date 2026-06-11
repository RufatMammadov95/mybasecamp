document.addEventListener("DOMContentLoaded", function() {
    var form = document.getElementById("newProjectFormContainer");
    var grid = document.querySelector(".projects-grid");

    var formStatus = localStorage.getItem("projectFormStatus");

    if (formStatus === "open") {
        if (form)
            form.style.display = "block";
        if (grid)
            grid.style.display = "none";
    } else {
        if (form)
            form.style.display = "none";
        if (grid)
            grid.style.display = "flex";
    }
});

function toggleProjectForm() {
    var form = document.getElementById("newProjectFormContainer");
    var grid = document.querySelector(".projects-grid");

    if (form.style.display === "none" || form.style.display === "") {
        form.style.display = "block";
        if (grid)
            grid.style.display = "none";

        localStorage.setItem("projectFormStatus", "open");
    } else {
        form.style.display = "none";
        if (grid)
            grid.style.display = "flex";

        localStorage.setItem("projectFormStatus", "closed");
    }
}
function validateProjectForm() {
    const nameInput = document.getElementById('name');
    const descInput = document.getElementById('description');

    if (nameInput.value.trim().length < 3) {
        alert("Project name must be at least 3 characters long and cannot be just spaces!");
        nameInput.focus();
        return false;
    }

    if (descInput.value.trim().length < 10) {
        alert("Project description must be at least 10 characters long!");
        descInput.focus();
        return false;
    }

    localStorage.setItem('projectFormStatus', 'closed');
    return true;
}
