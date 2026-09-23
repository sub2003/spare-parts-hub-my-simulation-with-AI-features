(function () {
    'use strict';

    var reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');

    function prefersReducedMotion() {
        return reducedMotionQuery.matches;
    }

    function initAutofocus() {
        document.querySelectorAll('[data-autofocus]').forEach(function (element) {
            if (window.innerWidth >= 768) {
                element.focus({preventScroll: true});
            }
        });
    }

    function initMobileNav() {
        document.querySelectorAll('.app-navbar .nav-link').forEach(function (link) {
            link.addEventListener('click', function () {
                var nav = document.getElementById('navMenu');
                if (!nav || window.innerWidth >= 1200 || !nav.classList.contains('show')) return;
                if (!window.bootstrap || !bootstrap.Collapse) return;
                bootstrap.Collapse.getOrCreateInstance(nav).hide();
            });
        });
    }

    function animateCounter(element) {
        var raw = element.textContent.trim();
        if (!/^-?\d+$/.test(raw)) return;

        var target = Number(raw);
        if (!Number.isFinite(target)) return;

        element.setAttribute('data-counter-ready', 'true');
        element.setAttribute('aria-label', raw);

        if (prefersReducedMotion() || Math.abs(target) > 999999) {
            element.textContent = raw;
            return;
        }

        var duration = 650;
        var startTime = null;
        var startValue = 0;

        function easeOutCubic(progress) {
            return 1 - Math.pow(1 - progress, 3);
        }

        function step(timestamp) {
            if (startTime === null) startTime = timestamp;
            var progress = Math.min((timestamp - startTime) / duration, 1);
            var value = Math.round(startValue + (target - startValue) * easeOutCubic(progress));
            element.textContent = value.toLocaleString();

            if (progress < 1) {
                window.requestAnimationFrame(step);
            } else {
                element.textContent = raw;
            }
        }

        element.textContent = '0';
        window.requestAnimationFrame(step);
    }

    function initCounters() {
        document.querySelectorAll('.metric-value:not(.metric-money)').forEach(function (element) {
            animateCounter(element);
        });
    }

    function initKpiOrbits() {
        var orbits = document.querySelectorAll('[data-kpi-orbit]');
        if (!orbits.length) return;

        orbits.forEach(function (orbit) {
            var metrics = orbit.querySelectorAll('.kpi-orbit__metric');
            metrics.forEach(function (metric, index) {
                metric.style.setProperty('--kpi-delay', (120 + index * 85) + 'ms');
                var value = metric.querySelector('.kpi-orbit__value');
                if (value && !value.hasAttribute('data-counter-ready')) animateCounter(value);
            });

            orbit.querySelectorAll('.kpi-orbit__value').forEach(function (value) {
                if (!value.hasAttribute('data-counter-ready')) animateCounter(value);
            });

            var panel = orbit.closest('.kpi-orbit-panel');
            if (panel) {
                window.requestAnimationFrame(function () {
                    window.requestAnimationFrame(function () {
                        panel.classList.add('kpi-orbit-ready');
                    });
                });
            }
        });
    }

    function initRevealAnimations() {
        if (prefersReducedMotion() || !('IntersectionObserver' in window)) return;

        var candidates = Array.from(document.querySelectorAll(
            '.content-section, .table-card, .form-card, .detail-card, .panel-card, .supplier-shell, .po-document'
        )).filter(function (element) {
            return !element.closest('.login-shell, .portal-login-shell');
        });

        if (!candidates.length) return;

        candidates.forEach(function (element) {
            element.setAttribute('data-reveal', '');
        });

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                entry.target.classList.add('is-revealed');
                observer.unobserve(entry.target);
            });
        }, {
            threshold: 0.08,
            rootMargin: '0px 0px -28px 0px'
        });

        document.documentElement.classList.add('motion-observer-ready');
        candidates.forEach(function (element) {
            observer.observe(element);
        });
    }

    function confirmationMarkup() {
        return '' +
            '<div class="modal fade app-confirm-modal" id="appConfirmModal" tabindex="-1" aria-labelledby="appConfirmTitle" aria-hidden="true">' +
            '  <div class="modal-dialog modal-dialog-centered">' +
            '    <div class="modal-content">' +
            '      <div class="modal-header">' +
            '        <div class="d-flex align-items-center gap-3">' +
            '          <span class="app-confirm-icon" aria-hidden="true">' +
            '            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3 21 20H3L12 3Z"/><path d="M12 9v5M12 17.2v.1"/></svg>' +
            '          </span>' +
            '          <div><div class="eyebrow mb-1">Confirm action</div><h2 class="modal-title fs-5" id="appConfirmTitle">Are you sure?</h2></div>' +
            '        </div>' +
            '        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
            '      </div>' +
            '      <div class="modal-body" id="appConfirmMessage">Please confirm this action.</div>' +
            '      <div class="modal-footer">' +
            '        <button type="button" class="btn btn-light" data-bs-dismiss="modal">Cancel</button>' +
            '        <button type="button" class="btn btn-danger" id="appConfirmButton">Confirm</button>' +
            '      </div>' +
            '    </div>' +
            '  </div>' +
            '</div>';
    }

    function initConfirmationModal() {
        var forms = Array.from(document.querySelectorAll('form')).filter(function (form) {
            return form.hasAttribute('data-confirm') || !!form.querySelector('button[data-confirm], [type="submit"][data-confirm]');
        });
        if (!forms.length) return;

        var bootstrapAvailable = window.bootstrap && bootstrap.Modal;
        if (!bootstrapAvailable) {
            forms.forEach(function (form) {
                form.addEventListener('submit', function (event) {
                    var source = event.submitter && event.submitter.hasAttribute('data-confirm') ? event.submitter : form;
                    if (!window.confirm(source.getAttribute('data-confirm') || 'Are you sure?')) {
                        event.preventDefault();
                    }
                });
            });
            return;
        }

        if (!document.getElementById('appConfirmModal')) {
            document.body.insertAdjacentHTML('beforeend', confirmationMarkup());
        }

        var modalElement = document.getElementById('appConfirmModal');
        var modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        var title = document.getElementById('appConfirmTitle');
        var message = document.getElementById('appConfirmMessage');
        var confirmButton = document.getElementById('appConfirmButton');
        var activeForm = null;
        var activeSubmitter = null;

        forms.forEach(function (form) {
            form.addEventListener('submit', function (event) {
                if (form.dataset.confirmApproved === 'true') {
                    delete form.dataset.confirmApproved;
                    return;
                }

                event.preventDefault();
                activeForm = form;
                activeSubmitter = event.submitter || null;

                var source = activeSubmitter && activeSubmitter.hasAttribute('data-confirm') ? activeSubmitter : form;
                var submitText = activeSubmitter ? activeSubmitter.textContent.trim() : '';
                var customTitle = source.getAttribute('data-confirm-title') || form.getAttribute('data-confirm-title');
                var customConfirm = source.getAttribute('data-confirm-button') || form.getAttribute('data-confirm-button');
                var confirmMessage = source.getAttribute('data-confirm') || form.getAttribute('data-confirm') || 'Are you sure you want to continue?';
                var tone = source.getAttribute('data-confirm-tone') || form.getAttribute('data-confirm-tone');
                var danger = tone === 'danger' ||
                    !!form.querySelector('.btn-danger, .btn-outline-danger') ||
                    /delete|remove|withdraw|reject|deactivate/i.test(submitText + ' ' + confirmMessage);

                title.textContent = customTitle || (submitText ? 'Confirm ' + submitText : 'Confirm action');
                message.textContent = confirmMessage;
                confirmButton.textContent = customConfirm || submitText || 'Confirm';
                confirmButton.classList.toggle('btn-danger', danger);
                confirmButton.classList.toggle('btn-primary', !danger);
                modal.show();
            });
        });

        confirmButton.addEventListener('click', function () {
            if (!activeForm) return;
            activeForm.dataset.confirmApproved = 'true';
            modal.hide();

            if (typeof activeForm.requestSubmit === 'function') {
                activeSubmitter ? activeForm.requestSubmit(activeSubmitter) : activeForm.requestSubmit();
            } else {
                activeForm.submit();
            }
        });

        modalElement.addEventListener('hidden.bs.modal', function () {
            activeForm = null;
            activeSubmitter = null;
        });
    }

    function initAlerts() {
        document.querySelectorAll('.alert').forEach(function (alert) {
            if (!alert.hasAttribute('role')) alert.setAttribute('role', 'alert');
        });
    }

    function initStatusAccessibility() {
        document.querySelectorAll('.status, .status-badge, .status-pill').forEach(function (status) {
            if (!status.hasAttribute('aria-label')) {
                var text = status.textContent.trim();
                if (text) status.setAttribute('aria-label', 'Status: ' + text);
            }
        });
    }


    function initTypewriters() {
        var elements = document.querySelectorAll('[data-typewriter]');
        if (!elements.length) return;

        elements.forEach(function (element) {
            var raw = element.getAttribute('data-typewriter-phrases') || element.textContent.trim();
            var phrases = raw.split('|').map(function (value) { return value.trim(); }).filter(Boolean);
            if (!phrases.length) return;

            if (prefersReducedMotion()) {
                element.textContent = phrases[0];
                return;
            }

            var phraseIndex = 0;
            var charIndex = 0;
            var deleting = false;
            var timer = null;

            function schedule(delay) {
                window.clearTimeout(timer);
                timer = window.setTimeout(tick, delay);
            }

            function tick() {
                if (document.hidden) {
                    schedule(500);
                    return;
                }

                var phrase = phrases[phraseIndex];

                if (!deleting) {
                    charIndex += 1;
                    element.textContent = phrase.slice(0, charIndex);

                    if (charIndex >= phrase.length) {
                        deleting = true;
                        schedule(1800);
                        return;
                    }

                    schedule(54);
                    return;
                }

                charIndex -= 1;
                element.textContent = phrase.slice(0, Math.max(charIndex, 0));

                if (charIndex <= 0) {
                    deleting = false;
                    phraseIndex = (phraseIndex + 1) % phrases.length;
                    schedule(420);
                    return;
                }

                schedule(28);
            }

            element.textContent = '';
            schedule(260);
        });
    }


    function detectModuleClass() {
        var moduleElement = document.querySelector(
            '.module-inventory, .module-sales, .module-urgency, .module-warranty, .module-supplier, .module-reporting'
        );
        if (!moduleElement) return;

        ['inventory', 'sales', 'urgency', 'warranty', 'supplier', 'reporting'].some(function (name) {
            if (!moduleElement.classList.contains('module-' + name)) return false;
            document.body.classList.add('module-' + name + '-body');
            document.body.dataset.module = name;
            return true;
        });
    }

    function setMotionDelay(element, delay) {
        element.style.setProperty('--motion-delay', delay + 'ms');
    }

    function initPageChoreography() {
        document.documentElement.classList.add('motion-js');

        var page = document.querySelector('.page-wrap, .supplier-page, .portal-page');
        if (!page || page.closest('.login-page, .portal-login-page')) return;

        var head = page.querySelector(':scope > .page-head, :scope > .page-header');
        if (head) {
            head.classList.add('motion-enter');
            setMotionDelay(head, 0);
        }

        var prioritySelectors = [
            '.metric-grid', '.portal-summary', '.filter-bar', '.supplier-subnav',
            '.lifecycle', '.pos-layout', '.reporting-metric-grid'
        ];
        prioritySelectors.forEach(function (selector, index) {
            page.querySelectorAll(':scope > ' + selector).forEach(function (element) {
                element.classList.add('motion-enter-soft');
                setMotionDelay(element, 70 + (index * 35));
            });
        });

        var surfaces = Array.from(page.querySelectorAll(
            '.metric-card, .action-card, .table-card, .form-card, .detail-card, .panel-card, .panel, .receipt-card, .receipt-item, .po-document, .report-rule-card'
        ));
        surfaces.forEach(function (surface, index) {
            surface.classList.add('motion-functional-surface');
            if (index < 10 && !surface.closest('.metric-grid, .portal-summary')) {
                surface.classList.add('motion-enter-soft');
                setMotionDelay(surface, 110 + Math.min(index, 8) * 45);
            }
        });

        page.querySelectorAll('.metric-icon, .module-icon').forEach(function (icon, index) {
            if (index < 10) icon.classList.add('motion-icon-glow');
        });
    }

    function initSmallTableMotion() {
        document.querySelectorAll('.table').forEach(function (table) {
            var rows = Array.from(table.querySelectorAll('tbody > tr')).filter(function (row) {
                return row.style.display !== 'none';
            });
            if (!rows.length) return;

            if (rows.length <= 12 && !table.closest('.audit-table, .report-audit-table')) {
                rows.forEach(function (row, index) {
                    row.classList.add('motion-table-row');
                    setMotionDelay(row, Math.min(index, 10) * 34);
                });
            } else {
                var container = table.closest('.table-responsive, .table-card');
                if (container) container.classList.add('motion-enter-soft');
            }
        });
    }

    function initFormMicroInteractions() {
        document.querySelectorAll('.form-control, .form-select, .form-check-input').forEach(function (field) {
            var group = field.closest('.mb-3, .mb-4, [class*="col-"], .form-section, .input-group');
            if (!group) return;

            field.addEventListener('focus', function () {
                group.classList.add('motion-field-active');
            });
            field.addEventListener('blur', function () {
                group.classList.remove('motion-field-active');
            });

            field.addEventListener('invalid', function () {
                field.classList.remove('motion-invalid');
                void field.offsetWidth;
                field.classList.add('motion-invalid');
            });
        });
    }

    function initLoadingButtons() {
        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (!(form instanceof HTMLFormElement)) return;
            if ((form.getAttribute('method') || 'get').toLowerCase() !== 'post') return;
            if (form.hasAttribute('data-no-loading')) return;

            window.setTimeout(function () {
                if (event.defaultPrevented) return;
                if (form.dataset.motionSubmitting === 'true') return;

                form.dataset.motionSubmitting = 'true';
                var submitter = event.submitter || form.querySelector('button[type="submit"], input[type="submit"]');
                if (!submitter || submitter.classList.contains('btn-close')) return;
                submitter.classList.add('is-loading');
                submitter.setAttribute('aria-busy', 'true');
            }, 0);
        });

        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (!(form instanceof HTMLFormElement)) return;
            if (form.dataset.motionSubmittedOnce === 'true' && !event.defaultPrevented) {
                event.preventDefault();
                return;
            }
            if (!event.defaultPrevented && (form.getAttribute('method') || 'get').toLowerCase() === 'post') {
                form.dataset.motionSubmittedOnce = 'true';
            }
        });
    }

    function initProgressMotion() {
        if (prefersReducedMotion()) return;

        document.querySelectorAll('.score-fill').forEach(function (bar) {
            var target = bar.style.width || getComputedStyle(bar).width;
            if (!target) return;
            bar.classList.add('motion-progress-fill');
            bar.style.width = '0%';
            window.requestAnimationFrame(function () {
                window.requestAnimationFrame(function () {
                    bar.style.width = target;
                });
            });
        });

        document.querySelectorAll('.progress-bar').forEach(function (bar) {
            var target = bar.style.width;
            if (!target) return;
            bar.classList.add('motion-progress-fill');
            bar.style.width = '0%';
            window.requestAnimationFrame(function () {
                window.requestAnimationFrame(function () { bar.style.width = target; });
            });
        });
    }

    function initWorkflowMotion() {
        document.querySelectorAll('.lifecycle-step').forEach(function (step, index) {
            step.classList.add('motion-workflow-step');
            setMotionDelay(step, index * 85);
        });

        document.querySelectorAll('.status').forEach(function (status, index) {
            if (index < 16) status.classList.add('motion-status-entry');
        });
    }

    function initScanMotion() {
        var qrInput = document.querySelector('.module-inventory input[type="file"][accept*="image"]');
        if (!qrInput) return;
        var card = qrInput.closest('.form-card');
        if (card) card.classList.add('scan-motion-card');
    }

    function initPosMotion() {
        document.querySelectorAll('.cart-line').forEach(function (line, index) {
            line.classList.add('motion-cart-line');
            setMotionDelay(line, Math.min(index, 8) * 45);
        });

        var cartTotal = document.querySelector('.cart-total');
        if (cartTotal) cartTotal.classList.add('motion-total-pop');
    }

    function initSuccessMotion() {
        document.querySelectorAll('.alert-success').forEach(function (alert) {
            alert.classList.add('motion-success-alert');
        });
    }

    function initHorizontalScrollers() {
        document.querySelectorAll('.workspace-grid, .portal-summary').forEach(function (container) {
            if (container.children.length >= 3) container.classList.add('motion-horizontal-scroller');
        });
    }

    function initInteractiveRows() {
        document.querySelectorAll('.table tbody tr').forEach(function (row) {
            row.addEventListener('focusin', function () { row.classList.add('motion-row-highlight'); });
            row.addEventListener('animationend', function (event) {
                if (event.animationName === 'row-update-highlight') row.classList.remove('motion-row-highlight');
            });
        });
    }


    function initReceivingProgress() {
        document.querySelectorAll('input[id^="acceptedQuantity-"][type="number"]').forEach(function (input) {
            var max = Number(input.getAttribute('max') || 0);
            if (!Number.isFinite(max) || max <= 0) return;

            var host = input.parentElement;
            var progress = document.createElement('div');
            progress.className = 'receive-progress';
            progress.setAttribute('aria-hidden', 'true');
            progress.innerHTML = '<span class="receive-progress-fill"></span>';
            host.appendChild(progress);

            var fill = progress.querySelector('.receive-progress-fill');
            function update() {
                var value = Math.max(0, Math.min(Number(input.value || 0), max));
                fill.style.width = ((value / max) * 100) + '%';
                progress.title = value + ' of ' + max + ' remaining units accepted';
            }
            input.addEventListener('input', update);
            update();
        });

        document.querySelectorAll('textarea[id^="serialNumbers-"]').forEach(function (textarea) {
            var id = textarea.id.replace('serialNumbers-', 'acceptedQuantity-');
            var accepted = document.getElementById(id);
            if (!accepted) return;

            var feedback = document.createElement('div');
            feedback.className = 'serial-motion-feedback';
            feedback.setAttribute('aria-live', 'polite');
            textarea.insertAdjacentElement('afterend', feedback);

            function update() {
                var count = textarea.value.split(/\r?\n/).map(function (v) { return v.trim(); }).filter(Boolean).length;
                var acceptedCount = Number(accepted.value || 0);
                feedback.textContent = count + ' serial' + (count === 1 ? '' : 's') + ' entered / ' + acceptedCount + ' accepted';
                feedback.classList.toggle('is-over', count > acceptedCount);
                feedback.classList.toggle('is-match', count > 0 && count === acceptedCount);
            }
            textarea.addEventListener('input', update);
            accepted.addEventListener('input', update);
            update();
        });
    }

    function initRmaMotion() {
        var page = document.querySelector('.module-warranty');
        if (!page || !/RMA-/.test((page.querySelector('.page-title') || {}).textContent || '')) return;
        var summary = page.querySelector('.detail-card');
        if (!summary) return;

        var statuses = Array.from(summary.querySelectorAll('.section-header .status')).map(function (el) {
            return el.textContent.trim().toLowerCase().replace(/\s+/g, '_');
        });
        if (!statuses.length) return;

        var claimStatus = statuses[0] || 'pending';
        var resolution = statuses[1] || 'pending';
        var steps = [
            {key:'opened', label:'Opened'},
            {key:'review', label: claimStatus === 'rejected' ? 'Rejected' : 'Reviewed'},
            {key:'resolution', label:'Resolution'},
            {key:'closed', label:'Closed'}
        ];
        var activeIndex = 0;
        if (claimStatus === 'approved' || claimStatus === 'rejected' || claimStatus === 'closed') activeIndex = 1;
        if (resolution !== 'pending') activeIndex = 2;
        if (claimStatus === 'closed') activeIndex = 3;

        var flow = document.createElement('div');
        flow.className = 'rma-motion-flow';
        flow.setAttribute('aria-label', 'RMA workflow progress');
        steps.forEach(function (step, index) {
            var node = document.createElement('div');
            node.className = 'rma-motion-step' + (index < activeIndex ? ' is-complete' : (index === activeIndex ? ' is-current' : ''));
            if (claimStatus === 'rejected' && index === 1) node.classList.add('is-rejected');
            node.innerHTML = '<span class="rma-motion-dot">' + (index < activeIndex ? '✓' : (index + 1)) + '</span><span>' + step.label + '</span>';
            flow.appendChild(node);
        });
        summary.querySelector('.section-header').insertAdjacentElement('afterend', flow);
    }


    function initSalesDiscountPreview() {
        document.querySelectorAll('.discount-line-form').forEach(function (form) {
            var discountInput = form.querySelector('.discount-amount-input');
            var reasonInput = form.querySelector('.discount-reason-input');
            var finalPrice = form.querySelector('.discount-final-price');
            var lineTotal = form.querySelector('.discount-line-total');
            var quantityInput = form.querySelector('input[name="quantity"]');
            var catalog = Number(form.dataset.catalogPrice || 0);
            if (!discountInput || !finalPrice || !lineTotal || !quantityInput) return;

            function formatMoney(value) {
                var safe = Number.isFinite(value) ? Math.max(0, value) : 0;
                return 'LKR ' + safe.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
            }

            function refreshPreview() {
                var discount = Number(discountInput.value || 0);
                var quantity = Number(quantityInput.value || 0);
                if (!Number.isFinite(discount)) discount = 0;
                if (!Number.isFinite(quantity)) quantity = 0;
                var final = Math.max(0, catalog - discount);
                finalPrice.textContent = formatMoney(final);
                lineTotal.textContent = formatMoney(final * Math.max(0, quantity));
                if (reasonInput) reasonInput.required = discount > 0;
                discountInput.setCustomValidity(discount > catalog ? 'Discount cannot exceed the unit price.' : '');
            }

            discountInput.addEventListener('input', refreshPreview);
            quantityInput.addEventListener('input', refreshPreview);
            refreshPreview();
        });
    }

    function initDynamicCartObserver() {
        var cart = document.querySelector('.pos-cart');
        if (!cart || !('MutationObserver' in window)) return;
        var observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                mutation.addedNodes.forEach(function (node) {
                    if (!(node instanceof HTMLElement)) return;
                    if (node.classList.contains('cart-line')) {
                        node.classList.add('motion-cart-line');
                        setMotionDelay(node, 0);
                    }
                });
            });
        });
        observer.observe(cart, {childList: true, subtree: true});
    }



    function initFunctionalMotionDecor() {
        var page = document.querySelector('.page-wrap, .supplier-page, .portal-page');
        if (!page || page.closest('.login-page, .portal-login-page')) return;

        if (!document.querySelector('.functional-motion-rail')) {
            ['rail-left', 'rail-right'].forEach(function (side) {
                var rail = document.createElement('span');
                rail.className = 'functional-motion-rail ' + side;
                rail.setAttribute('aria-hidden', 'true');
                document.body.appendChild(rail);
            });

            var orbit = document.createElement('span');
            orbit.className = 'functional-orbit-node';
            orbit.setAttribute('aria-hidden', 'true');
            document.body.appendChild(orbit);
        }
    }

    function initSurfaceEdgeMotion() {
        var continuous = document.querySelectorAll(
            '.metric-card, .action-card, .workflow-step, .recommend-card'
        );
        continuous.forEach(function (surface, index) {
            surface.classList.add('motion-edge-continuous');
            surface.style.setProperty('--motion-card-delay', ((index % 6) * -.55) + 's');
            if (surface.querySelector(':scope > .motion-edge-runner')) return;
            var runner = document.createElement('span');
            runner.className = 'motion-edge-runner';
            runner.setAttribute('aria-hidden', 'true');
            surface.appendChild(runner);
        });

        var reactive = document.querySelectorAll(
            '.table-card, .form-card, .detail-card, .panel-card, .panel, .receipt-card, .receipt-item, .po-document, .supplier-shell, .report-rule-card'
        );
        reactive.forEach(function (surface) {
            if (surface.classList.contains('motion-edge-continuous')) return;
            surface.classList.add('motion-edge-reactive');
            if (surface.querySelector(':scope > .motion-edge-runner')) return;
            var runner = document.createElement('span');
            runner.className = 'motion-edge-runner';
            runner.setAttribute('aria-hidden', 'true');
            surface.appendChild(runner);
        });
    }

    function initRichSectionChoreography() {
        var page = document.querySelector('.page-wrap, .supplier-page, .portal-page');
        if (!page || page.closest('.login-page, .portal-login-page')) return;

        var selectors = [
            '.section-header', '.table-card-header', '.form-section', '.info-box',
            '.detail-item', '.empty-state', '.lifecycle', '.workflow', '.receipt-item'
        ];
        var index = 0;
        selectors.forEach(function (selector) {
            page.querySelectorAll(selector).forEach(function (element) {
                if (index >= 34) return;
                element.classList.add('motion-section-enter');
                setMotionDelay(element, 90 + ((index % 10) * 42));
                index += 1;
            });
        });

        page.querySelectorAll('.workflow-step').forEach(function (step, stepIndex) {
            step.style.setProperty('--workflow-delay', (stepIndex * .52) + 's');
        });
    }

    function initButtonRipples() {
        document.addEventListener('pointerdown', function (event) {
            if (prefersReducedMotion()) return;
            var button = event.target.closest('.btn');
            if (!button || button.disabled || button.classList.contains('disabled')) return;

            var rect = button.getBoundingClientRect();
            var ripple = document.createElement('span');
            ripple.className = 'motion-click-ripple';
            ripple.style.left = (event.clientX - rect.left) + 'px';
            ripple.style.top = (event.clientY - rect.top) + 'px';
            ripple.setAttribute('aria-hidden', 'true');
            button.appendChild(ripple);
            window.setTimeout(function () { ripple.remove(); }, 680);
        });
    }

    function initFieldChangeFeedback() {
        document.querySelectorAll('.form-control, .form-select, .form-check-input').forEach(function (field) {
            field.addEventListener('change', function () {
                var group = field.closest('.mb-3, .mb-4, [class*="col-"], .form-section, .input-group') || field.parentElement;
                if (!group) return;
                group.classList.remove('motion-field-changed');
                void group.offsetWidth;
                group.classList.add('motion-field-changed');
                window.setTimeout(function () { group.classList.remove('motion-field-changed'); }, 620);
            });
        });
    }

    function initFilterResultMotion() {
        var timers = new WeakMap();
        document.querySelectorAll('input[type="search"], [data-table-search], .filter-bar input, .filter-bar select').forEach(function (field) {
            var eventName = field.tagName === 'SELECT' ? 'change' : 'input';
            field.addEventListener(eventName, function () {
                var oldTimer = timers.get(field);
                if (oldTimer) window.clearTimeout(oldTimer);
                timers.set(field, window.setTimeout(function () {
                    var host = field.closest('.supplier-shell, .page-wrap, .supplier-page, .portal-page') || document;
                    var target = host.querySelector('.table-card, .panel-card.p-0, .table-responsive');
                    if (!target) return;
                    target.classList.remove('motion-results-refresh');
                    void target.offsetWidth;
                    target.classList.add('motion-results-refresh');
                }, 90));
            });
        });
    }

    function initDynamicValueMotion() {
        if (!('MutationObserver' in window)) return;
        var values = document.querySelectorAll('[data-po-total], .cart-total, .score-value, .serial-motion-feedback, .search-result-count');
        values.forEach(function (value) {
            var observer = new MutationObserver(function () {
                if (prefersReducedMotion()) return;
                value.classList.remove('motion-value-updated');
                void value.offsetWidth;
                value.classList.add('motion-value-updated');
            });
            observer.observe(value, {childList: true, characterData: true, subtree: true});
        });
    }

    function initActionRowFeedback() {
        document.querySelectorAll('.table tbody tr, .table-modern tbody tr').forEach(function (row) {
            row.addEventListener('click', function (event) {
                if (!event.target.closest('a, button, input, select, textarea, label')) return;
                row.classList.remove('motion-row-highlight');
                void row.offsetWidth;
                row.classList.add('motion-row-highlight');
            });
        });
    }



    function initReferenceMotionV4() {
        var path = (window.location.pathname || '').toLowerCase();
        var body = document.body;

        if (!body.dataset.module) {
            var module = 'dashboard';
            if (path.indexOf('/inventory') === 0) module = 'inventory';
            else if (path.indexOf('/sales') === 0) module = 'sales';
            else if (path.indexOf('/stockmonitoring') === 0 || path.indexOf('/stock-requests') >= 0) module = 'urgency';
            else if (path.indexOf('/warranty') === 0) module = 'warranty';
            else if (path.indexOf('/supplier-portal') === 0) module = 'portal';
            else if (path.indexOf('/supplier') === 0) module = 'supplier';
            else if (path.indexOf('/reporting') === 0) module = 'reporting';
            else if (path.indexOf('/dashboard') === 0 || path === '/' || path === '') module = 'dashboard';
            body.dataset.module = module;
            body.classList.add('module-' + module + '-body');
        }

        if (!document.querySelector('.v4-ambient-stage')) {
            var stage = document.createElement('div');
            stage.className = 'v4-ambient-stage';
            stage.setAttribute('aria-hidden', 'true');
            stage.innerHTML = '<span class="v4-ambient-orb orb-a"></span><span class="v4-ambient-orb orb-b"></span><span class="v4-ambient-orb orb-c"></span>';
            body.insertBefore(stage, body.firstChild);
        }

        var page = document.querySelector('.page-wrap, .supplier-page, .portal-page, .dashboard-page');
        if (page && !page.closest('.login-page, .portal-login-page')) {
            page.classList.add('v4-page-enter');
            Array.from(page.children).slice(0, 12).forEach(function (child, index) {
                child.style.setProperty('--v4-seq-delay', Math.min(index, 9) * 55 + 'ms');
            });
        }

        var head = document.querySelector('.page-head, .page-header, .dashboard-hero-copy, .supplier-page-header, .portal-page-header');
        if (head) {
            head.classList.add('v4-module-head');
            if (!head.querySelector('.v4-floating-badges')) {
                var badges = document.createElement('div');
                badges.className = 'v4-floating-badges';
                badges.setAttribute('aria-hidden', 'true');

                var moduleName = (body.dataset.module || 'workspace').replace(/(^|[-_])\w/g, function (m) { return m.replace(/[-_]/,' ').toUpperCase(); });
                var hasPrimaryOrbit = !!document.querySelector('[data-kpi-orbit]');
                var firstMetric = hasPrimaryOrbit ? null : document.querySelector('.metric-card .metric-value, .portal-summary .metric-value, .reporting-metric-grid .metric-value');
                var firstLabel = firstMetric ? firstMetric.closest('.metric-card, .summary-card, .reporting-metric-card') : null;
                var labelNode = firstLabel ? firstLabel.querySelector('.metric-label, .metric-title, .card-label') : null;
                var labelText = labelNode ? labelNode.textContent : '';
                var metricText = firstMetric ? firstMetric.textContent.trim() : '';

                badges.innerHTML = '<span class="v4-floating-badge"><strong>●</strong> Live workspace</span>' +
                    '<span class="v4-floating-badge"><strong>' + moduleName + '</strong></span>' +
                    (!hasPrimaryOrbit && metricText && labelText ? '<span class="v4-floating-badge"><strong>' + metricText + '</strong> ' + labelText.trim() + '</span>' : '');
                head.appendChild(badges);
            }
        }

        var surfaces = document.querySelectorAll(
            '.metric-card, .action-card, .table-card, .form-card, .detail-card, .panel-card, .panel, .receipt-card, .receipt-item, .po-document, .report-rule-card, .supplier-shell, .workflow-step, .lifecycle-step, .empty-state'
        );
        surfaces.forEach(function (surface, index) {
            surface.classList.add('v4-animated-surface');
            surface.style.setProperty('--v4-surface-index', index);
        });

        document.querySelectorAll('.metric-icon, .module-icon, .action-icon, .nav-link svg, .nav-link i, .status-icon').forEach(function (icon) {
            icon.classList.add('v4-icon-alive');
        });

        document.querySelectorAll('.table, .table-modern').forEach(function (table) {
            table.classList.add('v4-table-alive');
        });

        document.querySelectorAll('form').forEach(function (form) {
            form.classList.add('v4-form-alive');
        });

        document.querySelectorAll('.btn').forEach(function (button) {
            if (!button.classList.contains('btn-close')) button.classList.add('v4-button-alive');
        });

        document.querySelectorAll('.status, .status-badge, .status-pill').forEach(function (status) {
            var text = status.textContent.trim().toLowerCase();
            if (/active|pending|critical|warning|shipped|processing|open|low stock/.test(text)) {
                status.classList.add('v4-status-alive');
            }
        });

        document.querySelectorAll('.lifecycle, .workflow, .rma-motion-flow, .po-lifecycle, .receive-progress').forEach(function (flow) {
            flow.classList.add('v4-workflow-alive');
        });
    }

    function initAiDemandCharts() {
        document.querySelectorAll('.ai-demand-chart[data-ai-history]').forEach(function (chart) {
            var raw = (chart.dataset.aiHistory || '').split(',').map(function (v) { return Number(v.trim()); }).filter(function (v) { return Number.isFinite(v) && v >= 0; });
            var forecastAvg = Number(chart.dataset.aiForecast || 0);
            var historyPath = chart.querySelector('.ai-chart-history-path');
            var forecastPath = chart.querySelector('.ai-chart-forecast-path');
            var split = chart.querySelector('.ai-chart-split');
            if (!historyPath || !forecastPath || raw.length < 2) return;

            var values = raw.concat([forecastAvg]);
            var max = Math.max.apply(null, values.concat([1]));
            var minX = 34, maxX = 700, topY = 20, bottomY = 198;
            var splitX = 470;
            var historyWidth = splitX - minX;
            function y(value) { return bottomY - ((value / max) * (bottomY - topY)); }
            function historyX(index) { return minX + (index / Math.max(1, raw.length - 1)) * historyWidth; }

            var d = '';
            raw.forEach(function (value, index) {
                d += (index === 0 ? 'M ' : ' L ') + historyX(index).toFixed(1) + ' ' + y(value).toFixed(1);
            });
            historyPath.setAttribute('d', d);

            var lastY = y(raw[raw.length - 1]);
            var forecastY = y(forecastAvg);
            forecastPath.setAttribute('d', 'M ' + splitX + ' ' + lastY.toFixed(1) + ' L 530 ' + forecastY.toFixed(1) + ' L ' + maxX + ' ' + forecastY.toFixed(1));
            if (split) {
                split.setAttribute('cx', splitX);
                split.setAttribute('cy', lastY.toFixed(1));
            }
        });
    }

    function init() {
        detectModuleClass();
        initReferenceMotionV4();
        initPageChoreography();
        initAutofocus();
        initMobileNav();
        initCounters();
        initKpiOrbits();
        initAiDemandCharts();
        initTypewriters();
        initRevealAnimations();
        initConfirmationModal();
        initAlerts();
        initStatusAccessibility();
        initSmallTableMotion();
        initFormMicroInteractions();
        initLoadingButtons();
        initProgressMotion();
        initWorkflowMotion();
        initScanMotion();
        initPosMotion();
        initSuccessMotion();
        initHorizontalScrollers();
        initInteractiveRows();
        initReceivingProgress();
        initRmaMotion();
        initSalesDiscountPreview();
        initDynamicCartObserver();
        initFunctionalMotionDecor();
        initSurfaceEdgeMotion();
        initRichSectionChoreography();
        initButtonRipples();
        initFieldChangeFeedback();
        initFilterResultMotion();
        initDynamicValueMotion();
        initActionRowFeedback();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
