using ClimbBetter.Domain.Interfaces;
using ClimbBetter.Domain.Training;
using ClimbBetter.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace ClimbBetter.Infrastructure.Repositories
{
    public class QualityRepository : IQualityRepository
    {

        private readonly ClimbBetterDbContext _dbContext;

        public QualityRepository(ClimbBetterDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<List<Quality>> GetAllAsync(CancellationToken cancellationToken)
        {
            return await _dbContext.Qualities
                .OrderBy(q => q.Id)
                .ToListAsync(cancellationToken);
        }

        public async Task<Quality?> GetByIdAsync(
            int id,
            CancellationToken cancellationToken)
        {
            return await _dbContext.Qualities
                .FirstOrDefaultAsync(q => q.Id == id, cancellationToken);
        }
    }
}