import React, { useEffect, useMemo, useRef, useState } from 'react';
import { agentDashboardService } from '../api/agentDashboardService';
import { DashboardEmployeStatus } from '../types';
import Badge from '../components/ui/Badge';
import Select from '../components/ui/Select';
import { API_BASE } from '../api/axios';

const formatTime = (time: string | null | undefined): string => {
  if (!time) return '--:--';
  // time peut être "09:12:17.066" ou "09:12:17" — on garde HH:mm:ss
  return time.substring(0, 8);
};

const SuiviTempsReelPage: React.FC = () => {
  const [statuses, setStatuses] = useState<DashboardEmployeStatus[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatut, setFilterStatut] = useState<string>('all');
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const uniqueStatuses = useMemo(() => {
    const byEmployeId = new Map<number, DashboardEmployeStatus>();
    for (const status of statuses) {
      if (!byEmployeId.has(status.employeId)) {
        byEmployeId.set(status.employeId, status);
      }
    }
    return Array.from(byEmployeId.values());
  }, [statuses]);

  useEffect(() => {
    loadStatuses();
    // Polling toutes les 10 secondes
    intervalRef.current = setInterval(loadStatuses, 10000);
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, []);

  const loadStatuses = async () => {
    try {
      const res = await agentDashboardService.getDashboard();
      setStatuses(res.data.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const filteredStatuses = uniqueStatuses.filter((s) => {
    const matchSearch = `${s.nom} ${s.prenom} ${s.poste || ''} ${s.departement || ''}`
      .toLowerCase()
      .includes(searchTerm.toLowerCase());
    const matchStatut = filterStatut === 'all' || s.statut === filterStatut;
    return matchSearch && matchStatut;
  });

  const getStatutBadge = (statut: string, agentActif: boolean) => {
    switch (statut) {
      case 'PRESENT':            return <Badge variant="success">Présent</Badge>;
      case 'RETARD':             return <Badge variant="warning">Retard</Badge>;
      case 'ABSENT':             return <Badge variant="danger">Absent</Badge>;
      case 'INCOMPLET':          return <Badge variant="neutral">Incomplet</Badge>;
      case 'EN_CONGE':           return <Badge variant="info">En congé</Badge>;
      case 'TELETRAVAIL':        return <Badge variant="success">Télétravail</Badge>;
      case 'JOUR_FERIE':         return <Badge variant="neutral">Jour férié</Badge>;
      case 'JOUR_NON_TRAVAILLE': return <Badge variant="neutral">Non travaillé</Badge>;
      case 'EN_AUTORISATION':    return <Badge variant="info">Autorisation</Badge>;
      default:                   return <Badge variant="neutral">{statut}</Badge>;
    }
  };

  const getAgentIndicator = (agentActif: boolean) => (
    <span className={`inline-block w-3 h-3 rounded-full ${agentActif ? 'bg-green-500 animate-pulse' : 'bg-gray-400'}`}
      title={agentActif ? 'Agent actif' : 'Agent inactif'}
    />
  );

  const stats = {
    presents: uniqueStatuses.filter(s => s.statut === 'PRESENT').length,
    retards: uniqueStatuses.filter(s => s.statut === 'RETARD').length,
    absents: uniqueStatuses.filter(s => s.statut === 'ABSENT').length,
    conges: uniqueStatuses.filter(s => s.statut === 'EN_CONGE').length,
    teletravail: uniqueStatuses.filter(s => s.statut === 'TELETRAVAIL').length,
    nonTravailles: uniqueStatuses.filter(s => s.statut === 'JOUR_FERIE' || s.statut === 'JOUR_NON_TRAVAILLE').length,
    agentsActifs: uniqueStatuses.filter(s => s.agentActif).length,
    surReseau: uniqueStatuses.filter(s => s.surReseauEntreprise).length,
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">Suivi Temps Réel</h1>
          <p className="text-sm text-gray-500 mt-1">Mise à jour automatique toutes les 10 secondes</p>
        </div>
      </div>

      {/* Statistiques globales */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3 mb-6">
        <div className="bg-green-50 dark:bg-green-900/20 rounded-xl p-4 border border-green-200 dark:border-green-800">
          <p className="text-xs text-green-600 dark:text-green-400">Présents</p>
          <p className="text-3xl font-bold text-green-700 dark:text-green-300">{stats.presents}</p>
        </div>
        <div className="bg-orange-50 dark:bg-orange-900/20 rounded-xl p-4 border border-orange-200 dark:border-orange-800">
          <p className="text-xs text-orange-600 dark:text-orange-400">En retard</p>
          <p className="text-3xl font-bold text-orange-700 dark:text-orange-300">{stats.retards}</p>
        </div>
        <div className="bg-red-50 dark:bg-red-900/20 rounded-xl p-4 border border-red-200 dark:border-red-800">
          <p className="text-xs text-red-600 dark:text-red-400">Absents réels</p>
          <p className="text-3xl font-bold text-red-700 dark:text-red-300">{stats.absents}</p>
        </div>
        <div className="bg-sky-50 dark:bg-sky-900/20 rounded-xl p-4 border border-sky-200 dark:border-sky-800">
          <p className="text-xs text-sky-600 dark:text-sky-400">En congé</p>
          <p className="text-3xl font-bold text-sky-700 dark:text-sky-300">{stats.conges}</p>
        </div>
        <div className="bg-teal-50 dark:bg-teal-900/20 rounded-xl p-4 border border-teal-200 dark:border-teal-800">
          <p className="text-xs text-teal-600 dark:text-teal-400">Télétravail</p>
          <p className="text-3xl font-bold text-teal-700 dark:text-teal-300">{stats.teletravail}</p>
        </div>
        <div className="bg-gray-50 dark:bg-gray-700/30 rounded-xl p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-xs text-gray-500 dark:text-gray-400">Fériés / N/T</p>
          <p className="text-3xl font-bold text-gray-600 dark:text-gray-300">{stats.nonTravailles}</p>
        </div>
        <div className="bg-blue-50 dark:bg-blue-900/20 rounded-xl p-4 border border-blue-200 dark:border-blue-800">
          <p className="text-xs text-blue-600 dark:text-blue-400">Agents actifs</p>
          <p className="text-3xl font-bold text-blue-700 dark:text-blue-300">{stats.agentsActifs}</p>
        </div>
        <div className="bg-purple-50 dark:bg-purple-900/20 rounded-xl p-4 border border-purple-200 dark:border-purple-800">
          <p className="text-xs text-purple-600 dark:text-purple-400">Sur réseau</p>
          <p className="text-3xl font-bold text-purple-700 dark:text-purple-300">{stats.surReseau}</p>
        </div>
      </div>

      {/* Filtres */}
      <div className="flex gap-3 mb-4">
        <input
          type="text"
          placeholder="Rechercher un employé..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition"
        />
        <Select
            value={filterStatut}
            onChange={setFilterStatut}
            className="w-52"
            options={[
              { value: 'all', label: 'Tous les statuts' },
              { value: 'PRESENT', label: 'Présent' },
              { value: 'RETARD', label: 'En retard' },
              { value: 'ABSENT', label: 'Absent' },
              { value: 'EN_CONGE', label: 'En congé' },
              { value: 'TELETRAVAIL', label: 'Télétravail' },
              { value: 'EN_AUTORISATION', label: 'Autorisation' },
              { value: 'JOUR_FERIE', label: 'Jour férié' },
              { value: 'JOUR_NON_TRAVAILLE', label: 'Jour non travaillé' },
              { value: 'INCOMPLET', label: 'Incomplet' },
            ]}
          />
      </div>

      {loading ? (
        <div className="text-center py-12 text-gray-500">Chargement...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredStatuses.map((emp) => (
            <div
              key={emp.employeId}
              className="bg-white dark:bg-gray-800 rounded-3xl shadow p-3 border border-gray-200 dark:border-gray-700 hover:shadow-lg transition-shadow"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  {emp.imageUrl ? (
                    <img src={`${API_BASE}${emp.imageUrl}`} alt="" className="w-10 h-10 rounded-full object-cover" />
                  ) : (
                    <div className="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900 flex items-center justify-center text-blue-600 dark:text-blue-300 font-semibold text-sm">
                      {emp.nom?.charAt(0)}{emp.prenom?.charAt(0)}
                    </div>
                  )}
                  <div>
                    <p className="font-semibold text-gray-800 dark:text-white">{emp.nom} {emp.prenom}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{emp.poste || 'N/A'} - {emp.departement || 'N/A'}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {getAgentIndicator(emp.agentActif)}
                  {getStatutBadge(emp.statut, emp.agentActif)}
                </div>
              </div>
              <div className="mt-2 grid grid-cols-5 gap-1 text-center border-t border-gray-100 dark:border-gray-700 pt-2">
                <div>
                  <p className="text-[10px] text-gray-400 dark:text-gray-500">Entrée</p>
                  <p className="text-xs font-medium dark:text-gray-200">{formatTime(emp.heureEntree)}</p>
                </div>
                <div>
                  <p className="text-[10px] text-gray-400 dark:text-gray-500">Retard</p>
                  <p className={`text-xs font-medium ${emp.retardMinutes > 0 ? 'text-orange-500' : 'dark:text-gray-200'}`}>
                    {emp.retardMinutes || 0}m
                  </p>
                </div>
                <div>
                  <p className="text-[10px] text-gray-400 dark:text-gray-500">WiFi</p>
                  <p className="text-[10px] font-medium dark:text-gray-200 truncate" title={emp.ssidConnecte || ''}>
                    {emp.ssidConnecte || 'N/A'}
                  </p>
                </div>
                <div>
                  <p className="text-[10px] text-gray-400 dark:text-gray-500">Réseau</p>
                  <p className={`text-[10px] font-medium ${emp.surReseauEntreprise ? 'text-green-600' : 'text-red-500'}`}>
                    {emp.surReseauEntreprise ? '✅' : '❌ Ext.'}
                  </p>
                </div>
                <div>
                  <p className="text-[10px] text-gray-400 dark:text-gray-500">Act/Inact</p>
                  <p className="text-[10px] font-medium dark:text-gray-200">
                    {emp.tempsActifMinutes || 0}/{emp.tempsInactifMinutes || 0}m
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default SuiviTempsReelPage;
