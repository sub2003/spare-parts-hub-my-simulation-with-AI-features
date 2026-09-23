(function () {
    'use strict';

    function initTableSearch() {
        document.querySelectorAll('[data-table-search]').forEach(function (input) {
            var tableId = input.getAttribute('data-table-search');
            var table = document.getElementById(tableId);
            if (!table) return;

            var rows = Array.from(table.querySelectorAll('tbody tr[data-search-row]'));
            var container = input.closest('.search-box, .search-input-wrap') || input.parentElement;
            var result = document.createElement('span');
            result.className = 'search-result-count';
            result.setAttribute('aria-live', 'polite');

            var clearButton = document.createElement('button');
            clearButton.type = 'button';
            clearButton.className = 'search-clear-btn';
            clearButton.setAttribute('aria-label', 'Clear search');
            clearButton.innerHTML = '&times;';

            if (container) {
                container.appendChild(clearButton);
                container.insertAdjacentElement('afterend', result);
            } else {
                input.insertAdjacentElement('afterend', result);
            }

            function update() {
                var needle = input.value.trim().toLowerCase();
                var visible = 0;

                rows.forEach(function (row) {
                    var matches = !needle || row.textContent.toLowerCase().includes(needle);
                    row.style.display = matches ? '' : 'none';
                    row.classList.toggle('search-match-visible', matches && !!needle);
                    if (matches) visible += 1;
                });

                if (container) container.classList.toggle('has-value', !!needle);
                result.textContent = needle
                    ? visible + ' of ' + rows.length + ' shown'
                    : rows.length + (rows.length === 1 ? ' record' : ' records');
                result.classList.remove('motion-count-change');
                void result.offsetWidth;
                result.classList.add('motion-count-change');

                var tableHost = table.closest('.panel-card, .table-card, .table-responsive') || table;
                tableHost.classList.remove('motion-results-refresh');
                void tableHost.offsetWidth;
                tableHost.classList.add('motion-results-refresh');
            }

            input.addEventListener('input', update);
            clearButton.addEventListener('click', function () {
                input.value = '';
                input.focus();
                update();
            });

            update();
        });
    }

    function updatePoTotal() {
        var quantity = document.querySelector('[data-po-quantity]');
        var price = document.querySelector('[data-po-price]');
        var output = document.querySelector('[data-po-total]');

        if (!quantity || !price || !output) return;

        var q = Number(quantity.value || 0);
        var p = Number(price.value || 0);
        var total = q * p;

        var nextText = Number.isFinite(total)
            ? total.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            })
            : '0.00';

        if (output.textContent !== nextText) {
            output.textContent = nextText;
            output.classList.remove('supplier-total-updated');
            void output.offsetWidth;
            output.classList.add('supplier-total-updated');
        } else {
            output.textContent = nextText;
        }
    }

    function initPoTotal() {
        document.querySelectorAll('[data-po-quantity], [data-po-price]').forEach(function (element) {
            element.addEventListener('input', updatePoTotal);
        });
        updatePoTotal();
    }

    function initCharacterCounters() {
        document.querySelectorAll('[data-char-count]').forEach(function (field) {
            var target = document.getElementById(field.getAttribute('data-char-count'));
            if (!target) return;

            function update() {
                target.textContent = field.value.length;
            }

            field.addEventListener('input', update);
            update();
        });
    }

    initTableSearch();
    initPoTotal();
    initCharacterCounters();
})();
