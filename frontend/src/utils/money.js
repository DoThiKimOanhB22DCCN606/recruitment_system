const vietnameseNumber = new Intl.NumberFormat('vi-VN', {
  maximumFractionDigits: 20,
});

export const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') return '';
  const amount = Number(value);
  return Number.isFinite(amount) ? vietnameseNumber.format(amount) : String(value);
};

export const formatMoneyInput = (value) => {
  const cleaned = String(value ?? '').replace(/[^\d,]/g, '');
  const [integer, ...fraction] = cleaned.split(',');
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return fraction.length ? `${grouped},${fraction.join('')}` : grouped;
};

export const parseMoneyInput = (value) => {
  const normalized = String(value ?? '').replace(/\./g, '').replace(',', '.');
  return normalized === '' ? null : Number(normalized);
};
