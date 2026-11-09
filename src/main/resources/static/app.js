// Весь интерфейс работает с API исходного Spring Boot проекта.
// JWT хранится в sessionStorage: закрытие вкладки завершает локальный сеанс.
const $ = (id) => document.getElementById(id);
const roleNames = { ADMIN: 'Администратор', TEACHER: 'Преподаватель', SUPER_ADMIN: 'Суперадминистратор' };
let session = null;
let section = 'students';
let records = [];
let editingId = null;
let readOnly = false;
let loading = false;
let saving = false;
let loadNumber = 0;
let expiryTimer;

function hasRole(role) {
    return session && session.roles.includes(role);
}

function endSession(message = '') {
    session = null;
    records = [];
    loadNumber++;
    clearTimeout(expiryTimer);
    sessionStorage.removeItem('student-session');
    $('editor').close();
    $('workspace').hidden = true;
    $('login-view').hidden = false;
    $('logout').hidden = true;
    $('login-error').textContent = message;
    $('login-form').elements.password.value = '';
}

// Единое место для запросов и обработки ошибок сервера.
async function api(path, method = 'GET', body) {
    if (session && Date.now() >= session.expiresAt) {
        endSession('Время сеанса истекло. Войдите снова.');
        throw new Error('Время сеанса истекло.');
    }
    const headers = {};
    if (session) headers.Authorization = 'Bearer ' + session.token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    let response;
    try {
        response = await fetch('/api/v1/' + path, {
            method, headers, body: body === undefined ? undefined : JSON.stringify(body)
        });
    } catch {
        throw new Error('Нет связи с сервером. Проверьте, запущено ли приложение, и повторите попытку.');
    }
    const text = await response.text();
    let data;
    try { data = text ? JSON.parse(text) : null; } catch { data = null; }
    if (!response.ok) {
        if (path === 'auth/login' && [400, 401, 403].includes(response.status)) {
            throw new Error('Проверьте email и пароль.');
        }
        if (response.status === 401) {
            endSession('Войдите в систему снова.');
            throw new Error('Сеанс завершён.');
        }
        const messages = {
            403: 'Нет доступа к этому действию. Если права были изменены, войдите заново.',
            404: 'Запись не найдена. Обновите список.',
            409: 'Запись с такими данными уже существует.',
            500: 'Сервер не выполнил запрос. Проверьте данные (в том числе уникальность email) и повторите попытку.'
        };
        throw new Error(messages[response.status] || data?.message || 'Ошибка запроса: ' + response.status);
    }
    return data;
}

function notify(message, error = false) {
    $('notice').textContent = message;
    $('notice').className = error ? 'notice error' : 'notice';
    $('notice').hidden = !message;
}

function showWorkspace() {
    $('login-view').hidden = true;
    $('workspace').hidden = false;
    $('logout').hidden = false;
    $('account').textContent = session.email;
    $('role-badge').textContent = session.roles.map(role => roleNames[role] || role).join(', ');
    $('sections').hidden = !(hasRole('SUPER_ADMIN') && (hasRole('ADMIN') || hasRole('TEACHER')));
    clearTimeout(expiryTimer);
    expiryTimer = setTimeout(() => endSession('Время сеанса истекло. Войдите снова.'), session.expiresAt - Date.now());
    changeSection(hasRole('ADMIN') || hasRole('TEACHER') ? 'students' : 'users');
}

function changeSection(value) {
    section = value;
    $('students-tab').setAttribute('aria-pressed', section === 'students');
    $('users-tab').setAttribute('aria-pressed', section === 'users');
    const users = section === 'users';
    $('page-title').textContent = users ? 'Пользователи' : 'Студенты';
    $('list-title').textContent = users ? 'Учётные записи' : 'Список студентов';
    $('page-description').textContent = users ? 'Управляйте учётными записями и правами доступа.' : 'Просматривайте записи и поддерживайте контакты в актуальном состоянии.';
    $('add').textContent = users ? '+ Добавить пользователя' : '+ Добавить студента';
    $('add').hidden = !users && !hasRole('ADMIN');
    $('search').placeholder = users ? 'Найти по email или ID…' : 'Найти по имени, фамилии или ID…';
    $('search').value = '';
    $('table-foot').textContent = users ? 'Email определяет пользователя при изменении ролей. Пароль задаётся при создании.' : hasRole('ADMIN') ? 'Откройте карточку, чтобы изменить телефон или email студента.' : 'Режим просмотра. Изменение записей доступно администратору.';
    $('table-head').replaceChildren();
    const row = document.createElement('tr');
    for (const title of users ? ['ID', 'Email', 'Роли', 'Действия'] : ['ID', 'Имя', 'Фамилия', 'Действия']) {
        const th = document.createElement('th');
        th.scope = 'col';
        th.textContent = title;
        row.append(th);
    }
    $('table-head').append(row);
    notify('');
    loadRecords();
}

async function loadRecords() {
    const requestNumber = ++loadNumber;
    loading = true;
    records = [];
    $('refresh').disabled = true;
    $('add').disabled = true;
    renderTable();
    let failed = false;
    try {
        const data = await api(section === 'users' ? 'users' : 'student');
        if (requestNumber !== loadNumber || !session) return;
        records = section === 'users' ? data.responses : data.response;
    } catch (error) {
        if (requestNumber !== loadNumber || !session) return;
        failed = true;
        notify(error.message, true);
    } finally {
        if (requestNumber === loadNumber) {
            loading = false;
            $('refresh').disabled = false;
            $('add').disabled = false;
            renderTable();
            if (failed) {
                $('empty').hidden = true;
                $('count').textContent = 'Не удалось загрузить';
            }
        }
    }
}

function addCell(row, value, className = '') {
    const cell = document.createElement('td');
    cell.textContent = value ?? '—';
    cell.className = className;
    row.append(cell);
    return cell;
}

function rowButton(cell, label, action, className = 'secondary') {
    const button = document.createElement('button');
    button.textContent = label;
    button.className = className;
    button.addEventListener('click', async () => {
        button.disabled = true;
        try { await action(); } catch (error) { notify(error.message, true); }
        finally { button.disabled = false; }
    });
    cell.append(button);
}

function renderTable() {
    const query = $('search').value.trim().toLocaleLowerCase('ru');
    const users = section === 'users';
    const filtered = records.filter(item => (users ? [item.id, item.email] : [item.id, item.name, item.surname])
        .some(value => String(value ?? '').toLocaleLowerCase('ru').includes(query)));
    $('table-body').replaceChildren();
    $('count').textContent = loading ? 'Загрузка…' : `Показано: ${filtered.length} из ${records.length}`;
    $('empty').hidden = loading || filtered.length > 0;
    for (const item of filtered) {
        const row = document.createElement('tr');
        addCell(row, '#' + item.id, 'id-cell');
        addCell(row, users ? item.email : item.name);
        if (users) {
            const cell = addCell(row, '');
            for (const role of item.roles) {
                const badge = document.createElement('span');
                badge.className = 'badge';
                badge.textContent = roleNames[role] || role;
                cell.append(badge);
            }
        } else addCell(row, item.surname);
        const actions = addCell(row, '');
        const group = document.createElement('div');
        group.className = 'row-actions';
        actions.append(group);
        rowButton(group, users ? 'Изменить роли' : hasRole('ADMIN') ? 'Карточка / изменить' : 'Карточка', () => openEditor(item.id));
        if (users || hasRole('ADMIN')) {
            rowButton(group, 'Удалить', () => deleteRecord(item), 'danger');
        }
        $('table-body').append(row);
    }
}

async function deleteRecord(item) {
    const users = section === 'users';
    if (users && item.email === session.email) {
        notify('Нельзя удалить свою учётную запись из этого интерфейса.', true);
        return;
    }
    const name = users ? item.email : `${item.name} ${item.surname}`;
    if (!window.confirm(`Удалить запись «${name}»? Это действие нельзя отменить.`)) return;
    await api((users ? 'users/' : 'student/') + item.id, 'DELETE');
    notify('Запись удалена.');
    await loadRecords();
}

async function openEditor(id = null) {
    const users = section === 'users';
    const openedSection = section;
    const openedSession = session;
    let item = {};
    if (id !== null) {
        item = users ? records.find(record => record.id === id) : await api('student/' + id);
        if (openedSession !== session || openedSection !== section) return;
    }
    editingId = id;
    readOnly = !users && !hasRole('ADMIN');
    const form = $('editor-form');
    form.reset();
    $('form-error').textContent = '';
    $('student-fields').hidden = users;
    $('password-label').hidden = !users || id !== null;
    $('roles-field').hidden = !users;
    form.elements.name.disabled = users || id !== null;
    form.elements.surname.disabled = users || id !== null;
    form.elements.phoneNumber.disabled = users || readOnly;
    form.elements.email.disabled = (users && id !== null) || readOnly;
    form.elements.email.required = users;
    form.elements.password.disabled = !users || id !== null;
    for (const name of ['name', 'surname', 'phoneNumber', 'email']) form.elements[name].value = item[name] || '';
    for (const checkbox of form.querySelectorAll('[name="roles"]')) checkbox.checked = (item.roles || []).includes(checkbox.value);
    $('dialog-title').textContent = id === null ? users ? 'Новый пользователь' : 'Новый студент' : users ? 'Роли пользователя' : 'Карточка студента #' + id;
    $('dialog-description').textContent = users ? id === null ? 'Задайте email, пароль и права доступа.' : 'Email используется сервером для поиска записи. Здесь можно изменить только роли.' : id === null ? 'Укажите имя и фамилию. Контакты можно добавить позже.' : 'Имя и фамилия сохраняются при создании. Изменять можно только контакты.';
    $('save').hidden = readOnly;
    $('cancel').textContent = readOnly ? 'Закрыть' : 'Отмена';
    $('editor').showModal();
}

$('login-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector('button');
    button.disabled = true;
    button.textContent = 'Входим…';
    $('login-error').textContent = '';
    try {
        const email = form.elements.email.value.trim();
        const data = await api('auth/login', 'POST', { email, password: form.elements.password.value });
        session = { token: data.token, roles: data.roles.map(role => role.replace(/^ROLE_/, '')), email, expiresAt: Date.now() + data.expiresIn * 1000 };
        sessionStorage.setItem('student-session', JSON.stringify(session));
        form.elements.password.value = '';
        showWorkspace();
    } catch (error) { $('login-error').textContent = error.message; }
    finally { button.disabled = false; button.textContent = 'Войти в журнал →'; }
});

$('editor-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (saving || readOnly) return;
    const form = event.currentTarget;
    const users = section === 'users';
    const value = name => form.elements[name].value.trim();
    const roles = [...form.querySelectorAll('[name="roles"]:checked')].map(input => input.value);
    $('form-error').textContent = '';
    if (users && !roles.length) { $('form-error').textContent = 'Выберите хотя бы одну роль.'; return; }
    if (!users && editingId === null && (!value('name') || !value('surname'))) {
        $('form-error').textContent = 'Заполните имя и фамилию.'; return;
    }
    if (users && editingId !== null && value('email') === session.email && !roles.includes('SUPER_ADMIN')) {
        $('form-error').textContent = 'Нельзя убрать собственную роль суперадминистратора из этого интерфейса.'; return;
    }
    let body;
    if (users) {
        body = { email: value('email'), roles };
        if (editingId === null) body.password = form.elements.password.value;
    } else if (editingId === null) {
        // При создании API ждёт phone_number, при обновлении — phoneNumber.
        body = { name: value('name'), surname: value('surname'), phone_number: value('phoneNumber'), email: value('email') };
    } else body = { phoneNumber: value('phoneNumber'), email: value('email') };
    saving = true;
    $('save').disabled = true;
    $('save').textContent = 'Сохраняем…';
    try {
        const path = (users ? 'users/' : 'student/') + (editingId === null ? 'save' : editingId);
        await api(path, editingId === null ? 'POST' : 'PUT', body);
        $('editor').close();
        notify('Изменения сохранены.');
        await loadRecords();
    } catch (error) { $('form-error').textContent = error.message; }
    finally { saving = false; $('save').disabled = false; $('save').textContent = 'Сохранить'; }
});

$('logout').addEventListener('click', () => endSession());
$('refresh').addEventListener('click', () => { notify(''); loadRecords(); });
$('search').addEventListener('input', renderTable);
$('add').addEventListener('click', () => openEditor().catch(error => notify(error.message, true)));
$('students-tab').addEventListener('click', () => changeSection('students'));
$('users-tab').addEventListener('click', () => changeSection('users'));
for (const id of ['cancel', 'close-dialog']) $(id).addEventListener('click', () => { if (!saving) $('editor').close(); });
$('editor').addEventListener('cancel', event => { if (saving) event.preventDefault(); });

// Восстанавливаем только ещё действующий сеанс этой вкладки.
try {
    const saved = JSON.parse(sessionStorage.getItem('student-session'));
    if (saved && typeof saved.token === 'string' && Array.isArray(saved.roles) && Number.isFinite(saved.expiresAt) && saved.expiresAt > Date.now() && saved.expiresAt - Date.now() <= 3600000) {
        session = saved;
        showWorkspace();
    } else sessionStorage.removeItem('student-session');
} catch { sessionStorage.removeItem('student-session'); }
