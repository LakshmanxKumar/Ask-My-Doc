const STORAGE_KEY = "askMyDoc.documents";

/** Load the {id, name} list from localStorage. */
function loadDocuments() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (err) {
    console.error("Failed to read stored documents:", err);
    return [];
  }
}

function saveDocuments(docs) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(docs));
}

let documents = loadDocuments();

function renderDocuments() {
  const list = document.getElementById("document-list");
  const emptyState = document.getElementById("empty-state");
  list.innerHTML = "";

  if (documents.length === 0) {
    emptyState.hidden = false;
    return;
  }
  emptyState.hidden = true;

  documents.forEach((doc) => {
    const li = document.createElement("li");
    li.className = "document-item";

    const label = document.createElement("label");
    label.className = "document-select";

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.value = doc.id;

    const name = document.createElement("span");
    name.className = "document-name";
    name.textContent = doc.name;
    name.title = doc.name;

    label.appendChild(checkbox);
    label.appendChild(name);

    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.className = "remove-btn";
    removeBtn.textContent = "Remove";
    removeBtn.setAttribute("aria-label", `Remove ${doc.name} from list`);
    removeBtn.addEventListener("click", () => removeDocument(doc.id));

    li.appendChild(label);
    li.appendChild(removeBtn);
    list.appendChild(li);
  });
}

/** Removes from the local list only — does not call the backend. */
function removeDocument(id) {
  documents = documents.filter((d) => d.id !== id);
  saveDocuments(documents);
  renderDocuments();
}

/** Clears the local document list only — does not call the backend. */
const clearAllBtn = document.getElementById("clear-all-btn");

clearAllBtn.addEventListener("click", () => {
  if (documents.length === 0) return;

  const confirmed = confirm(
    "Clear all documents from this list? This only affects what's shown here " +
      "— it does not delete anything on the server."
  );
  if (!confirmed) return;

  documents = [];
  saveDocuments(documents);
  renderDocuments();
});

/* ---------- Upload ---------- */

const uploadForm = document.getElementById("upload-form");
const fileInput = document.getElementById("file-input");
const fileLabelText = document.getElementById("file-label-text");
const uploadStatus = document.getElementById("upload-status");
const uploadBtn = document.getElementById("upload-btn");

fileInput.addEventListener("change", () => {
  if (fileInput.files.length === 0) {
    fileLabelText.textContent = "Choose files";
  } else if (fileInput.files.length === 1) {
    fileLabelText.textContent = fileInput.files[0].name;
  } else {
    fileLabelText.textContent = `${fileInput.files.length} files selected`;
  }
});

uploadForm.addEventListener("submit", async (e) => {
  e.preventDefault();

  const files = Array.from(fileInput.files);
  if (files.length === 0) {
    uploadStatus.textContent = "Choose at least one file first.";
    return;
  }

  const formData = new FormData();
  files.forEach((file) => formData.append("file", file));

  uploadBtn.disabled = true;
  uploadStatus.textContent = "Uploading…";

  try {
    const res = await fetch("/api/v1/upload", {
      method: "POST",
      body: formData,
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(errBody || `Upload failed (${res.status})`);
    }

    const docIds = await res.json();

    // Assumes the backend returns ids in the same order the files were sent.
    if (!Array.isArray(docIds) || docIds.length !== files.length) {
      console.warn(
        "Number of returned ids did not match number of uploaded files; " +
          "pairing as far as possible."
      );
    }

    docIds.forEach((id, index) => {
      const file = files[index];
      if (!file) return;
      documents.push({ id, name: file.name });
    });

    saveDocuments(documents);
    renderDocuments();

    uploadStatus.textContent = `Uploaded ${docIds.length} document${
      docIds.length === 1 ? "" : "s"
    }.`;
    uploadForm.reset();
    fileLabelText.textContent = "Choose files";
  } catch (err) {
    console.error(err);
    uploadStatus.textContent = `Upload failed: ${err.message}`;
  } finally {
    uploadBtn.disabled = false;
  }
});

/* ---------- Query ---------- */

const queryForm = document.getElementById("query-form");
const queryInput = document.getElementById("query-input");
const queryStatus = document.getElementById("query-status");
const askBtn = document.getElementById("ask-btn");
const answerBlock = document.getElementById("answer-block");
const answerText = document.getElementById("answer-text");

queryForm.addEventListener("submit", async (e) => {
  e.preventDefault();

  const selectedIds = Array.from(
    document.querySelectorAll('#document-list input[type="checkbox"]:checked')
  ).map((cb) => cb.value);

  const question = queryInput.value.trim();

  if (selectedIds.length === 0) {
    queryStatus.textContent = "Select at least one document.";
    return;
  }
  if (!question) {
    queryStatus.textContent = "Type a question first.";
    return;
  }

  askBtn.disabled = true;
  queryStatus.textContent = "Thinking…";
  answerBlock.hidden = true;

  try {
    const res = await fetch("/api/v1/query", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userQuery: question, docIds: selectedIds }),
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(errBody || `Query failed (${res.status})`);
    }

    // /query returns a plain string body, not JSON.
    const answer = await res.text();
    answerText.textContent = answer;
    answerBlock.hidden = false;
    queryStatus.textContent = "";
  } catch (err) {
    console.error(err);
    queryStatus.textContent = `Something went wrong: ${err.message}`;
  } finally {
    askBtn.disabled = false;
  }
});

renderDocuments();
