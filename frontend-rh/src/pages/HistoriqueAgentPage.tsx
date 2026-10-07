import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  HiOutlineSearch,
  HiOutlineCalendar,
  HiOutlineUser,
  HiOutlineFilter,
  HiOutlineCheckCircle,
  HiOutlineClock,
  HiOutlineXCircle,
  HiOutlineBriefcase,
  HiOutlineDesktopComputer,
  HiOutlineViewList,
  HiOutlineViewBoards,
  HiOutlineBan,
  HiOutlineEyeOff,
  HiOutlineChevronDown,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
} from 'react-icons/hi';
import { agentHistoriqueService } from '../api/agentHistoriqueService';
import { employeService } from '../api/employeService';
import { Employe, HistoriqueEmploye, JourDetail } from '../types';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import { API_BASE } from '../api/axios';

const formatTime = (time: string | null | undefined): string => {
  if (!time) return '--:--';
  return time.substring(0, 8);
};

const getStatutBadge = (statut: string) => {
  switch (statut) {
    case 'PRESENT': return <Badge variant="success">Présent</Badge>;
    case 'RETARD': return <Badge variant="warning">Retard</Badge>;
    case 'ABSENT': return <Badge variant="danger">Absent</Badge>;
    case 'EN_CONGE': return <Badge variant="info">En congé</Badge>;
    case 'EN_AUTORISATION': return <Badge variant="info">Autorisation</Badge>;
    case 'TELETRAVAIL': return <Badge variant="success">Télétravail</Badge>;
    case 'JOUR_FERIE': return <Badge variant="neutral">Jour férié</Badge>;
    default: return <Badge variant="neutral">{statut}</Badge>;
  }
};

const getStatutLabel = (statut: string) => {
  switch (statut) {
    case 'PRESENT': return 'Présent';
    case 'RETARD': return 'Retard';
    case 'ABSENT': return 'Absent';
    case 'EN_CONGE': return 'En congé';
    case 'EN_AUTORISATION': return 'Autorisation';
    case 'TELETRAVAIL': return 'Télétravail';
    case 'JOUR_FERIE': return 'Jour férié';
    default: return statut;
  }
};

const getStatutColor = (statut: string): string => {
  switch (statut) {
    case 'PRESENT': return 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800';
    case 'RETARD': return 'bg-brand- dark:bg-brand-/20 border-brand- dark:border-brand-';
    case 'ABSENT': return 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800';
    case 'EN_CONGE': return 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800';
    case 'EN_AUTORISATION': return 'bg-indigo-50 dark:bg-indigo-900/20 border-indigo-200 dark:border-indigo-800';
    case 'TELETRAVAIL': return 'bg-teal-50 dark:bg-teal-900/20 border-teal-200 dark:border-teal-800';
    case 'JOUR_FERIE': return 'bg-gray-50 dark:bg-gray-800/50 border-gray-200 dark:border-gray-700';
    default: return 'bg-gray-50 dark:bg-gray-800 border-gray-200 dark:border-gray-700';
  }
};

const formatDate = (dateStr: string): string => {
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString('fr-FR', { weekday: 'short', day: '2-digit', month: 'short' });
};

const getMonthOptions = () => {
  const options = [];
  const now = new Date();
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const label = d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    options.push({ label: label.charAt(0).toUpperCase() + label.slice(1), value });
  }
  return options;
};

const formatLocalDate = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

type FilterOption = {
  value: string;
  label: string;
};

const FilterDropdown: React.FC<{
  label: string;
  icon: React.ReactNode;
  value: string;
  options: FilterOption[];
  placeholder: string;
  onChange: (value: string) => void;
  className?: string;
}> = ({ label, icon, value, options, placeholder, onChange, className = '' }) => {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      if (!wrapperRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  const selected = options.find((option) => option.value === value);

  return (
    <div ref={wrapperRef} className={className}>
      <label className="mb-1.5 flex items-center gap-1.5 text-sm font-medium text-gray-700 dark:text-gray-300">
        {icon}
        {label}
      </label>
      <div className="relative">
        <button
          type="button"
          onClick={() => setOpen((prev) => !prev)}
          className="flex h-11 w-full items-center justify-between rounded-xl border border-gray-300 bg-white px-4 text-left text-theme-sm text-gray-700 shadow-sm transition focus:border-brand-300 focus:outline-none focus:ring focus:ring-brand-500/10 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-300"
        >
          <span className="truncate">{selected?.label || placeholder}</span>
          <HiOutlineChevronDown
            size={18}
            className={`shrink-0 text-gray-400 transition-transform ${open ? 'rotate-180' : ''}`}
          />
        </button>

        {open && (
          <div className="absolute left-0 right-0 top-full z-50 mt-2 max-h-72 overflow-auto rounded-xl border border-gray-200 bg-white py-2 shadow-2xl ring-1 ring-black/5 dark:border-gray-700 dark:bg-gray-900">
            <button
              type="button"
              onClick={() => {
                onChange('');
                setOpen(false);
              }}
              className={`flex w-full items-center px-4 py-2.5 text-left text-theme-sm transition ${value === '' ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300' : 'text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-800'}`}
            >
              {placeholder}
            </button>

            {options.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => {
                  onChange(option.value);
                  setOpen(false);
                }}
                className={`flex w-full items-center px-4 py-2.5 text-left text-theme-sm transition ${value === option.value ? 'bg-brand-500 text-white' : 'text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-800'}`}
              >
                <span className="truncate">{option.label}</span>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

const HistoriqueAgentPage: React.FC = () => {
  const [employes, setEmployes] = useState<Employe[]>([]);
  const [selectedEmployeId, setSelectedEmployeId] = useState<number | null>(null);
  const [historique, setHistorique] = useState<HistoriqueEmploye | null>(null);
  const [loading, setLoading] = useState(false);
  const [filterStatut, setFilterStatut] = useState('');
  const [page, setPage] = useState(1);
  const pageSize = 10;

  const uniqueEmployes = useMemo(() => {
    const byIdentity = new Map<string, Employe>();
    for (const employe of employes) {
      const key = `${employe.nom}|${employe.prenom}|${employe.poste || 'N/A'}`
        .trim()
        .toLowerCase();
      if (!byIdentity.has(key)) {
        byIdentity.set(key, employe);
      }
    }
    return Array.from(byIdentity.values()).sort((a, b) => {
      const left = `${a.nom} ${a.prenom}`.toLowerCase();
      const right = `${b.nom} ${b.prenom}`.toLowerCase();
      return left.localeCompare(right, 'fr');
    });
  }, [employes]);

  const currentMonth = () => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  };
  const [selectedMonth, setSelectedMonth] = useState<string>(currentMonth());
  const monthOptions = useMemo(() => getMonthOptions(), []);
  const employeeOptions = useMemo(
    () => uniqueEmployes.map((emp) => ({
      value: String(emp.id),
      label: `${emp.nom} ${emp.prenom} - ${emp.poste || 'N/A'}`,
    })),
    [uniqueEmployes]
  );
  const statusOptions = useMemo(() => {
    const preferredOrder = ['ABSENT', 'RETARD', 'PRESENT', 'EN_CONGE', 'EN_AUTORISATION', 'TELETRAVAIL', 'JOUR_FERIE'];
    const presentStatuses = new Set((historique?.jours || []).map((jour) => jour.statut));
    return preferredOrder
      .filter((statut) => presentStatuses.has(statut))
      .map((statut) => ({ value: statut, label: getStatutLabel(statut) }));
  }, [historique]);

  const monthToRange = (ym: string) => {
    const [year, month] = ym.split('-').map(Number);
    const debut = formatLocalDate(new Date(year, month - 1, 1));
    const lastDay = new Date(year, month, 0);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const fin = formatLocalDate(lastDay <= today ? lastDay : today);
    return { debut, fin };
  };

  const { debut, fin } = monthToRange(selectedMonth);
  const [viewMode, setViewMode] = useState<'table' | 'timeline'>('table');

  useEffect(() => {
    employeService.getAll().then(res => {
      setEmployes(res.data.data || []);
    });
  }, []);

  const loadHistorique = async () => {
    if (!selectedEmployeId) return;
    setLoading(true);
    try {
      const res = await agentHistoriqueService.getHistorique(selectedEmployeId, debut, fin);
      setHistorique(res.data.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedEmployeId) loadHistorique();
  }, [selectedEmployeId, debut, fin]);

  useEffect(() => {
    setPage(1);
  }, [selectedEmployeId, selectedMonth, filterStatut]);

  const todayStr = formatLocalDate(new Date());

  const stats = historique ? {
    totalJours: historique.jours.filter(j => j.date <= todayStr).length,
    presents: historique.jours.filter(j => j.statut === 'PRESENT' && j.date <= todayStr).length,
    retards: historique.jours.filter(j => j.statut === 'RETARD' && j.date <= todayStr).length,
    absents: historique.jours.filter(j => j.statut === 'ABSENT' && j.date <= todayStr).length,
    conges: historique.jours.filter(j => j.statut === 'EN_CONGE' && j.date <= todayStr).length,
    teletravail: historique.jours.filter(j => j.statut === 'TELETRAVAIL' && j.date <= todayStr).length,
    feries: historique.jours.filter(j => j.statut === 'JOUR_FERIE' && j.date <= todayStr).length,
    totalRetardMin: historique.jours.filter(j => j.date <= todayStr).reduce((s, j) => s + (j.retardMinutes || 0), 0),
    totalActifMin: historique.jours.filter(j => j.date <= todayStr).reduce((s, j) => s + (j.tempsActifMinutes || 0), 0),
    totalInactifMin: historique.jours.filter(j => j.date <= todayStr).reduce((s, j) => s + (j.tempsInactifMinutes || 0), 0),
  } : null;

  const isWeekend = (dateStr: string) => {
    const d = new Date(dateStr + 'T00:00:00');
    const day = d.getDay();
    return day === 0 || day === 6; // 0 = dimanche, 6 = samedi
  };

  const filteredJours = useMemo(() => {
    const jours = (historique?.jours || []).filter(
      (jour) => !isWeekend(jour.date) && jour.statut !== 'JOUR_FERIE' && jour.date <= todayStr
    );
    if (!filterStatut) return jours;
    return jours.filter((jour) => jour.statut === filterStatut);
  }, [historique, filterStatut, todayStr]);

  const totalPages = Math.max(1, Math.ceil(filteredJours.length / pageSize));
  const paginatedJours = filteredJours.slice((page - 1) * pageSize, page * pageSize);

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">Historique Agent</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">Consultez l'historique détaillé de présence et d'activité de chaque employé</p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant={viewMode === 'table' ? 'primary' : 'outline'}
            size="sm"
            onClick={() => setViewMode('table')}
          >
            <HiOutlineViewList size={16} />
            Tableau
          </Button>
          <Button
            variant={viewMode === 'timeline' ? 'primary' : 'outline'}
            size="sm"
            onClick={() => setViewMode('timeline')}
          >
            <HiOutlineViewBoards size={16} />
            Timeline
          </Button>
        </div>
      </div>

      {/* Filtres */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-5">
        <div className="flex flex-wrap gap-4 items-end">
          <FilterDropdown
            label="Employé"
            icon={<HiOutlineUser size={15} className="text-gray-400" />}
            value={selectedEmployeId ? String(selectedEmployeId) : ''}
            options={employeeOptions}
            placeholder="-- Sélectionner un employé --"
            onChange={(value) => setSelectedEmployeId(value ? Number(value) : null)}
            className="min-w-[250px] flex-1"
          />
          <FilterDropdown
            label="Mois"
            icon={<HiOutlineCalendar size={15} className="text-gray-400" />}
            value={selectedMonth}
            options={monthOptions}
            placeholder="-- Sélectionner un mois --"
            onChange={setSelectedMonth}
            className="w-full min-w-[220px] sm:w-[240px]"
          />
          <FilterDropdown
            label="Statut"
            icon={<HiOutlineFilter size={15} className="text-gray-400" />}
            value={filterStatut}
            options={statusOptions}
            placeholder="Tous les statuts"
            onChange={setFilterStatut}
            className="w-full min-w-[220px] sm:w-[240px]"
          />
        </div>
      </div>

      {/* Empty state */}
      {!selectedEmployeId && !loading && (
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-16 text-center">
          <div className="flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-700 mx-auto mb-4">
            <HiOutlineSearch size={28} className="text-gray-400" />
          </div>
          <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">Sélectionnez un employé pour voir son historique</p>
          <p className="text-gray-400 dark:text-gray-500 text-sm mt-1">Utilisez le filtre ci-dessus pour choisir un employé</p>
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-12 text-center">
          <div className="animate-spin w-8 h-8 border-4 border-brand-500 border-t-transparent rounded-full mx-auto mb-3"></div>
          <p className="text-gray-500 dark:text-gray-400">Chargement de l'historique...</p>
        </div>
      )}

      {historique && !loading && (
        <>
          {/* Employee Info Header */}
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-5 flex items-center gap-4">
            {historique.imageUrl ? (
              <img src={`${API_BASE}${historique.imageUrl}`} alt="" className="w-14 h-14 rounded-full object-cover ring-2 ring-gray-200 dark:ring-gray-600" />
            ) : (
              <div className="w-14 h-14 rounded-full bg-brand-50 dark:bg-brand-900/30 flex items-center justify-center text-brand-600 dark:text-brand-400 font-bold text-lg ring-2 ring-brand-200 dark:ring-brand-800">
                {historique.nom?.charAt(0)}{historique.prenom?.charAt(0)}
              </div>
            )}
            <div className="flex-1">
              <h2 className="text-lg font-bold text-gray-800 dark:text-white">{historique.nom} {historique.prenom}</h2>
              <p className="text-sm text-gray-500 dark:text-gray-400 flex items-center gap-2">
                <HiOutlineBriefcase size={14} />
                {historique.poste || 'N/A'} — {historique.departement || 'N/A'}
              </p>
            </div>
            {stats && (
              <div className="hidden md:flex items-center gap-3 text-sm">
                <span className="text-gray-500 dark:text-gray-400">{stats.totalJours} jours</span>
                <span className="text-gray-300 dark:text-gray-600">•</span>
                <span className="text-gray-500 dark:text-gray-400">{filteredJours.length} affiché{filteredJours.length > 1 ? 's' : ''}</span>
              </div>
            )}
          </div>

          {/* Stats summary cards */}
          {stats && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <StatCard icon={<HiOutlineBan size={20} />} label="Total absences" value={`${stats.absents} jour${stats.absents > 1 ? 's' : ''}`} bgColor="bg-red-50 dark:bg-red-900/20" iconColor="text-red-500" borderColor="border-red-200 dark:border-red-800" valueColor="text-red-700 dark:text-red-300" />
              <StatCard icon={<HiOutlineClock size={20} />} label="Total retard" value={`${stats.totalRetardMin} min`} bgColor="bg-brand- dark:bg-brand-/20" iconColor="text-brand-" borderColor="border-brand- dark:border-brand-" valueColor="text-brand- dark:text-brand-" />
              <StatCard icon={<HiOutlineEyeOff size={20} />} label="Total inactif" value={`${stats.totalInactifMin} min`} bgColor="bg-purple-50 dark:bg-purple-900/20" iconColor="text-purple-500" borderColor="border-purple-200 dark:border-purple-800" valueColor="text-purple-700 dark:text-purple-300" />
            </div>
          )}

          {/* Table view */}
          {viewMode === 'table' && (
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 dark:bg-gray-900/50 border-b border-gray-200 dark:border-gray-700">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Date</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Statut</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Entrée</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Sortie</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Retard</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Actif</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Inactif</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">WiFi</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Réseau</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-gray-700/50">
                    {paginatedJours.map((jour, idx) => (
                      <tr key={idx} className="hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors">
                        <td className="px-4 py-3.5 font-medium text-gray-800 dark:text-gray-200 whitespace-nowrap">{formatDate(jour.date)}</td>
                        <td className="px-4 py-3.5">{getStatutBadge(jour.statut)}</td>
                        <td className="px-4 py-3.5 text-gray-700 dark:text-gray-300">{formatTime(jour.heureEntree)}</td>
                        <td className="px-4 py-3.5 text-gray-700 dark:text-gray-300">{formatTime(jour.heureSortie)}</td>
                        <td className="px-4 py-3.5">
                          <span className={jour.retardMinutes > 0 ? 'text-brand- font-semibold' : 'text-gray-400'}>
                            {jour.retardMinutes > 0 ? `${jour.retardMinutes} min` : '-'}
                          </span>
                        </td>
                        <td className="px-4 py-3.5 text-green-600 dark:text-green-400">{jour.tempsActifMinutes > 0 ? `${jour.tempsActifMinutes} min` : '-'}</td>
                        <td className="px-4 py-3.5 text-red-500">{jour.tempsInactifMinutes > 0 ? `${jour.tempsInactifMinutes} min` : '-'}</td>
                        <td className="px-4 py-3.5 text-gray-600 dark:text-gray-400 text-xs truncate max-w-[120px]" title={jour.ssid || ''}>{jour.ssid || '-'}</td>
                        <td className="px-4 py-3.5">
                          {jour.surReseauEntreprise ? (
                            <HiOutlineCheckCircle size={18} className="text-green-500" />
                          ) : jour.heureEntree ? (
                            <HiOutlineXCircle size={18} className="text-red-500" />
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </td>
                      </tr>
                    ))}
                    {paginatedJours.length === 0 && (
                      <tr>
                        <td colSpan={9} className="px-4 py-8 text-center text-sm text-gray-500 dark:text-gray-400">
                          Aucun jour ne correspond au statut sélectionné pour ce mois.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Timeline view */}
          {viewMode === 'timeline' && (
            <div className="space-y-3">
              {paginatedJours.map((jour, idx) => (
                <TimelineCard key={idx} jour={jour} />
              ))}
              {paginatedJours.length === 0 && (
                <div className="rounded-xl border border-gray-200 bg-white p-8 text-center text-sm text-gray-500 shadow-sm dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
                  Aucun jour ne correspond au statut sélectionné pour ce mois.
                </div>
              )}
            </div>
          )}

          {filteredJours.length > 8 && (
            <div className="flex flex-col gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 shadow-sm dark:border-gray-700 dark:bg-gray-800 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Affichage de <span className="font-medium text-gray-700 dark:text-gray-200">{(page - 1) * pageSize + 1}</span>
                {' '}à <span className="font-medium text-gray-700 dark:text-gray-200">{Math.min(page * pageSize, filteredJours.length)}</span>
                {' '}sur <span className="font-medium text-gray-700 dark:text-gray-200">{filteredJours.length}</span> jour{filteredJours.length > 1 ? 's' : ''}
              </p>
              <div className="flex items-center gap-2 self-end sm:self-auto">
                <button
                  type="button"
                  onClick={() => setPage((current) => Math.max(1, current - 1))}
                  disabled={page === 1}
                  className="inline-flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-600 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  <HiOutlineChevronLeft size={16} />
                  Précédent
                </button>
                <span className="min-w-[78px] text-center text-sm font-medium text-gray-700 dark:text-gray-200">
                  Page {page} / {totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => setPage((current) => Math.min(totalPages, current + 1))}
                  disabled={page === totalPages}
                  className="inline-flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-600 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  Suivant
                  <HiOutlineChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

const StatCard: React.FC<{
  icon: React.ReactNode;
  label: string;
  value: number | string;
  bgColor: string;
  iconColor: string;
  borderColor: string;
  valueColor: string;
}> = ({ icon, label, value, bgColor, iconColor, borderColor, valueColor }) => (
  <div className={`bg-white dark:bg-gray-800 rounded-xl shadow-sm border ${borderColor} p-4`}>
    <div className="flex items-center gap-3">
      <div className={`flex items-center justify-center w-10 h-10 rounded-lg ${bgColor}`}>
        <span className={iconColor}>{icon}</span>
      </div>
      <div>
        <p className={`text-2xl font-bold ${valueColor}`}>{value}</p>
        <p className="text-xs text-gray-500 dark:text-gray-400">{label}</p>
      </div>
    </div>
  </div>
);

const TimelineCard: React.FC<{ jour: JourDetail }> = ({ jour }) => {
  return (
    <div className={`bg-white dark:bg-gray-800 rounded-xl shadow-sm p-4 border ${getStatutColor(jour.statut)} transition hover:shadow-md`}>
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <HiOutlineCalendar size={16} className="text-gray-400" />
            <span className="text-sm font-bold text-gray-800 dark:text-white">{formatDate(jour.date)}</span>
          </div>
          {getStatutBadge(jour.statut)}
          {jour.teletravail && <Badge variant="info">🏠 Télétravail</Badge>}
        </div>
        {jour.retardMinutes > 0 && (
          <div className="flex items-center gap-1.5 text-sm font-semibold text-brand-">
            <HiOutlineClock size={16} />
            {jour.retardMinutes} min retard
          </div>
        )}
      </div>
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4 text-sm">
        <div className="bg-gray-50 dark:bg-gray-700/30 rounded-lg p-2.5">
          <span className="text-xs text-gray-500 dark:text-gray-400 block mb-0.5">Entrée</span>
          <p className="font-medium text-gray-800 dark:text-gray-200">{formatTime(jour.heureEntree)}</p>
        </div>
        <div className="bg-gray-50 dark:bg-gray-700/30 rounded-lg p-2.5">
          <span className="text-xs text-gray-500 dark:text-gray-400 block mb-0.5">Sortie</span>
          <p className="font-medium text-gray-800 dark:text-gray-200">{formatTime(jour.heureSortie)}</p>
        </div>
        <div className="bg-gray-50 dark:bg-gray-700/30 rounded-lg p-2.5">
          <span className="text-xs text-gray-500 dark:text-gray-400 block mb-0.5">Temps actif</span>
          <p className="font-medium text-green-600 dark:text-green-400">{jour.tempsActifMinutes > 0 ? `${jour.tempsActifMinutes} min` : '-'}</p>
        </div>
        <div className="bg-gray-50 dark:bg-gray-700/30 rounded-lg p-2.5">
          <span className="text-xs text-gray-500 dark:text-gray-400 block mb-0.5">Temps inactif</span>
          <p className="font-medium text-red-500">{jour.tempsInactifMinutes > 0 ? `${jour.tempsInactifMinutes} min` : '-'}</p>
        </div>
        <div className="bg-gray-50 dark:bg-gray-700/30 rounded-lg p-2.5">
          <span className="text-xs text-gray-500 dark:text-gray-400 block mb-0.5">WiFi / Réseau</span>
          <p className="font-medium text-gray-700 dark:text-gray-300 text-xs flex items-center gap-1">
            {jour.ssid || 'N/A'}
            {jour.surReseauEntreprise ? (
              <HiOutlineCheckCircle size={14} className="text-green-500" />
            ) : (
              <HiOutlineXCircle size={14} className="text-red-500" />
            )}
          </p>
        </div>
      </div>
    </div>
  );
};

export default HistoriqueAgentPage;
